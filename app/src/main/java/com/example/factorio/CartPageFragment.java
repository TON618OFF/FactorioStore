package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

            new Thread(() -> {
                try {
                    File pdfFile = generatePdf(orderId, email, items);
                    if (pdfFile.exists() && pdfFile.length() > 0) {
                        Log.i(TAG, "PDF файл успешно создан: " + pdfFile.getAbsolutePath() + ", размер: " + pdfFile.length());
                        sendEmailWithPdf(email, orderId, pdfFile);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Чек отправлен на " + email, Toast.LENGTH_SHORT).show();
                            cartManager.removeAllFromCart();
                            updateUI();
                        });
                    } else {
                        Log.e(TAG, "PDF файл не существует или пуст: " + pdfFile.getAbsolutePath());
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Ошибка: PDF файл не создан", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при отправке чека: " + e.getMessage(), e);
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка отправки чека", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void loadCart() {
        cartManager.loadCartFromFirestore(items -> {
            cartAdapter.notifyDataSetChanged();
            updateUI();
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

    private File generatePdf(String orderId, String email, List<CartItem> items) throws IOException {
        File pdfFile = new File(getContext().getCacheDir(), "receipt_" + orderId + ".pdf");
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

        for (CartItem item : items) {
            canvas.drawText(item.getName(), 20, yPosition, paint);
            canvas.drawText(String.valueOf(item.getQuantity()), 300, yPosition, paint);
            canvas.drawText(item.getPrice() + " руб.", 400, yPosition, paint);
            canvas.drawText(item.getTotalPrice() + " руб.", 500, yPosition, paint);
            yPosition += 20;
        }

        yPosition += 10;
        canvas.drawLine(20, yPosition, 575, yPosition, paint);
        yPosition += 20;

        canvas.drawText("Итого: " + calculateTotalPrice(items) + " руб.", 500, yPosition, paint);

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

    private void sendEmailWithPdf(String email, String orderId, File pdfFile) throws MessagingException, IOException {
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

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Спасибо за заказ! Чек во вложении.");

        MimeBodyPart attachmentPart = new MimeBodyPart();
        try {
            attachmentPart.attachFile(pdfFile);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка прикрепления файла: " + e.getMessage(), e);
            throw e;
        }

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
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
}