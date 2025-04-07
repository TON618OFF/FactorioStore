package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class CartPageFragment extends Fragment {

    private static final int CHECKOUT_REQUEST_CODE = 1;
    private static final String TAG = "CartPageFragment";

    private RecyclerView cartRecyclerView;
    private TextView cartItemsCount, cartTotalPrice;
    private ImageView backIcon;
    private ImageButton clearCartButton;
    private Button checkoutButton;
    private CartAdapter cartAdapter;
    private CartManager cartManager;
    private Handler mainHandler; // Handler для работы с главным потоком

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart_page, container, false);

        cartRecyclerView = view.findViewById(R.id.cart_recycler_view);
        cartItemsCount = view.findViewById(R.id.cart_items_count);
        cartTotalPrice = view.findViewById(R.id.cart_total_price);
        clearCartButton = view.findViewById(R.id.clear_cart_button);
        checkoutButton = view.findViewById(R.id.checkout_button);

        cartManager = CartManager.getInstance();
        cartAdapter = new CartAdapter(cartManager.getCartItems(), this::updateTotalPrice);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cartRecyclerView.setAdapter(cartAdapter);

        mainHandler = new Handler(Looper.getMainLooper()); // Инициализация Handler для главного потока

        loadCart();

        clearCartButton.setOnClickListener(v -> {
            cartManager.removeAllFromCart();
            updateUI();
        });

        checkoutButton.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(getContext(), "Войдите, чтобы оформить заказ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cartManager.getCartItems().isEmpty()) {
                Toast.makeText(getContext(), "Корзина пуста", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            intent.putExtra("cart_items", new ArrayList<>(cartManager.getCartItems()));
            startActivityForResult(intent, CHECKOUT_REQUEST_CODE);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECKOUT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String orderId = data.getStringExtra("order_id");
            String email = data.getStringExtra("email");
            List<CartItem> items = (List<CartItem>) data.getSerializableExtra("cart_items");
            int commission = data.getIntExtra("commission", 0);

            new Thread(() -> {
                try {
                    File pdfFile = generatePdf(orderId, email, items, commission);
                    if (pdfFile.exists() && pdfFile.length() > 0) {
                        Log.i(TAG, "PDF файл успешно создан: " + pdfFile.getAbsolutePath() + ", размер: " + pdfFile.length());
                        sendEmailWithPdf(email, orderId, pdfFile, commission, items);
                        saveOrderToHistory(orderId, email, items, commission);

                        // Безопасное обновление UI через Handler
                        if (isAdded()) { // Проверяем, что фрагмент прикреплён
                            mainHandler.post(() -> {
                                Toast.makeText(getContext(), "Чек отправлен на " + email, Toast.LENGTH_SHORT).show();
                                cartManager.removeAllFromCart();
                                updateUI();
                            });
                        }
                    } else {
                        Log.e(TAG, "PDF файл не существует или пуст: " + pdfFile.getAbsolutePath());
                        if (isAdded()) {
                            mainHandler.post(() ->
                                    Toast.makeText(getContext(), "Ошибка: PDF файл не создан", Toast.LENGTH_SHORT).show());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при отправке чека: " + e.getMessage(), e);
                    if (isAdded()) {
                        mainHandler.post(() ->
                                Toast.makeText(getContext(), "Ошибка отправки чека", Toast.LENGTH_SHORT).show());
                    }
                }
            }).start();
        }
    }

    private void saveOrderToHistory(String orderId, String email, List<CartItem> items, int commission) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        int subtotal = calculateTotalPrice(items);
        int totalWithCommission = subtotal + commission;

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", items);
        orderData.put("subtotal", subtotal);
        orderData.put("commission", commission);
        orderData.put("totalWithCommission", totalWithCommission);
        orderData.put("paymentMethod", commission > 0 ? "card" : "cash");

        db.collection("users").document(userId)
                .collection("orders_history").document(orderId)
                .set(orderData)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Заказ сохранён в историю: " + orderId))
                .addOnFailureListener(e -> Log.e(TAG, "Ошибка сохранения заказа в историю: " + e.getMessage()));
    }

    private void loadCart() {
        cartManager.loadCartFromFirestore(items -> {
            if (isAdded()) { // Проверяем, что фрагмент прикреплён
                cartAdapter.notifyDataSetChanged();
                updateUI();
            }
        });
    }

    private void updateUI() {
        List<CartItem> items = cartManager.getCartItems();
        int totalItems = 0;
        int totalPrice = 0;
        for (CartItem item : items) {
            totalItems += item.getQuantity();
            totalPrice += item.getTotalPrice();
        }
        cartItemsCount.setText("Товаров в корзине: " + totalItems);
        cartTotalPrice.setText("Общая цена: " + totalPrice + " руб.");
        cartAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        updateUI();
    }

    private File generatePdf(String orderId, String email, List<CartItem> items, int commission) throws IOException {
        File pdfFile = new File(requireContext().getCacheDir(), "receipt_" + orderId + ".pdf");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(16);
        paint.setFakeBoldText(true);

        int yPosition = 40;

        canvas.drawText("Чек заказа", 280, yPosition, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(12);
        yPosition += 20;

        canvas.drawText("Заказ #" + orderId, 20, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Дата: " + new java.util.Date().toString(), 20, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Пользователь: " + email, 20, yPosition, paint);
        yPosition += 30;

        canvas.drawText("Товар", 20, yPosition, paint);
        canvas.drawText("Кол-во", 300, yPosition, paint);
        canvas.drawText("Цена", 400, yPosition, paint);
        canvas.drawText("Сумма", 500, yPosition, paint);
        yPosition += 10;
        canvas.drawLine(20, yPosition, 575, yPosition, paint);
        yPosition += 20;

        int subtotal = 0;
        for (CartItem item : items) {
            canvas.drawText(item.getName(), 20, yPosition, paint);
            canvas.drawText(String.valueOf(item.getQuantity()), 300, yPosition, paint);
            canvas.drawText(item.getPrice() + " руб.", 400, yPosition, paint);
            canvas.drawText(item.getTotalPrice() + " руб.", 500, yPosition, paint);
            subtotal += item.getTotalPrice();
            yPosition += 20;
        }

        yPosition += 10;
        canvas.drawLine(20, yPosition, 575, yPosition, paint);
        yPosition += 20;

        canvas.drawText("Промежуточная сумма: " + subtotal + " руб.", 300, yPosition, paint);
        yPosition += 20;

        int totalWithCommission = subtotal;
        if (commission > 0) {
            canvas.drawText("Комиссия: " + commission + " руб.", 300, yPosition, paint);
            yPosition += 20;
            totalWithCommission = subtotal + commission;
        }

        canvas.drawText("Итого: " + totalWithCommission + " руб.", 300, yPosition, paint);

        pdfDocument.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка записи PDF: " + e.getMessage(), e);
            throw e;
        } finally {
            pdfDocument.close();
        }

        return pdfFile;
    }

    private void sendEmailWithPdf(String email, String orderId, File pdfFile, int commission, List<CartItem> items) throws MessagingException, IOException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication("factoriostore@gmail.com", "perv gdiy fjtb iely");
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("factoriostore@gmail.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Ваш чек заказа Factorio #" + orderId);

        int subtotal = calculateTotalPrice(items);
        int totalWithCommission = subtotal + (commission > 0 ? commission : 0);

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #2F2F2F; color: #D9D9D9; margin: 0; padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #4A4A4A; border-radius: 8px; padding: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.5); }" +
                "h1 { color: #5A769A; text-align: center; font-size: 24px; margin-bottom: 20px; }" +
                ".header { background-color: #3A4F7A; padding: 10px; border-radius: 4px; margin-bottom: 20px; }" +
                ".header p { color: #FFFFFF; margin: 5px 0; font-size: 14px; }" +
                "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
                "th, td { padding: 10px; text-align: left; border-bottom: 1px solid #5E6D70; }" +
                "th { background-color: #B87333; color: #FFFFFF; font-weight: bold; }" +
                "td { color: #D9D9D9; }" +
                ".summary { font-size: 14px; color: #D9D9D9; }" +
                ".summary span { float: right; }" +
                ".total { font-size: 18px; color: #FFC107; font-weight: bold; margin-top: 10px; }" +
                ".footer { text-align: center; font-size: 12px; color: #A6A29E; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1>Чек заказа Factorio #" + orderId + "</h1>" +
                "<div class='header'>" +
                "<p>Спасибо за заказ в Factorio Store!</p>" +
                "<p>Дата: " + new java.util.Date().toString() + "</p>" +
                "<p>Пользователь: " + email + "</p>" +
                "</div>" +
                "<table>" +
                "<tr>" +
                "<th>Товар</th>" +
                "<th>Кол-во</th>" +
                "<th>Цена</th>" +
                "<th>Сумма</th>" +
                "</tr>";

        for (CartItem item : items) {
            htmlContent += "<tr>" +
                    "<td>" + item.getName() + "</td>" +
                    "<td>" + item.getQuantity() + "</td>" +
                    "<td>" + item.getPrice() + " руб.</td>" +
                    "<td>" + item.getTotalPrice() + " руб.</td>" +
                    "</tr>";
        }

        htmlContent += "</table>" +
                "<div class='summary'>" +
                "<p>Промежуточная сумма: <span>" + subtotal + " руб.</span></p>";

        if (commission > 0) {
            htmlContent += "<p>Комиссия: <span>" + commission + " руб.</span></p>";
        }

        htmlContent += "<p class='total'>Итого: <span>" + totalWithCommission + " руб.</span></p>" +
                "</div>" +
                "<div style='text-align: center; margin-top: 20px;'>" +
                "<p style='color: #D9D9D9;'>Полный чек в формате PDF прикреплён ниже</p>" +
                "</div>" +
                "<div class='footer'>Factorio Store © 2025 | Индустрия будущего</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        MimeMultipart multipart = new MimeMultipart("mixed");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(pdfFile);
        attachmentPart.setFileName("receipt_" + orderId + ".pdf");
        attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        Log.i(TAG, "Попытка отправки email на " + email);
        Transport.send(message);
        Log.i(TAG, "Email успешно отправлен");
    }

    private int calculateTotalPrice(List<CartItem> items) {
        int totalPrice = 0;
        for (CartItem item : items) {
            totalPrice += item.getTotalPrice();
        }
        return totalPrice;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null); // Очистка Handler при уничтожении
    }
}