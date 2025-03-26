package com.example.factorio;

import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class CartPageFragment extends Fragment {

    private RecyclerView cartRecyclerView;
    private TextView cartItemsCount, cartTotalPrice;
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

        checkoutButton.setOnClickListener(v -> checkout());

        return view;
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

    private void checkout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Войдите, чтобы оформить заказ", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(getContext(), "Email не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<CartItem> items = cartManager.getCartItems();
        if (items.isEmpty()) {
            Toast.makeText(getContext(), "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", items);
        orderData.put("totalPrice", calculateTotalPrice(items));

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    Toast.makeText(getContext(), "Заказ оформлен! ID: " + orderId, Toast.LENGTH_SHORT).show();

                    new Thread(() -> {
                        try {
                            File pdfFile = generatePdf(orderId, email, items);
                            sendEmailWithPdf(email, orderId, pdfFile);
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Чек отправлен на " + email, Toast.LENGTH_SHORT).show();
                                cartManager.removeAllFromCart();
                                updateUI();
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при оформлении заказа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private File generatePdf(String orderId, String email, List<CartItem> items) throws Exception {
        File pdfFile = new File(getContext().getCacheDir(), "receipt_" + orderId + ".pdf");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 размер в точках (595x842)
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(16);
        paint.setFakeBoldText(true);

        int yPosition = 40; // Начальная позиция по Y

        // Заголовок
        canvas.drawText("Чек заказа", 280, yPosition, paint); // Центрируем по X (595 / 2)
        paint.setFakeBoldText(false);
        paint.setTextSize(12);
        yPosition += 20;

        canvas.drawText("Заказ #" + orderId, 20, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Дата: " + new java.util.Date().toString(), 20, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Пользователь: " + email, 20, yPosition, paint);
        yPosition += 30;

        // Заголовки таблицы
        canvas.drawText("Товар", 20, yPosition, paint);
        canvas.drawText("Кол-во", 300, yPosition, paint);
        canvas.drawText("Цена", 400, yPosition, paint);
        canvas.drawText("Сумма", 500, yPosition, paint);
        yPosition += 10;
        canvas.drawLine(20, yPosition, 575, yPosition, paint); // Линия под заголовками
        yPosition += 20;

        // Товары
        for (CartItem item : items) {
            canvas.drawText(item.getName(), 20, yPosition, paint);
            canvas.drawText(String.valueOf(item.getQuantity()), 300, yPosition, paint);
            canvas.drawText(item.getPrice() + " руб.", 400, yPosition, paint);
            canvas.drawText(item.getTotalPrice() + " руб.", 500, yPosition, paint);
            yPosition += 20;
        }

        yPosition += 10;
        canvas.drawLine(20, yPosition, 575, yPosition, paint); // Линия перед итогом
        yPosition += 20;

        // Итог
        canvas.drawText("Итого: " + calculateTotalPrice(items) + " руб.", 500, yPosition, paint);

        pdfDocument.finishPage(page);
        pdfDocument.writeTo(new FileOutputStream(pdfFile));
        pdfDocument.close();

        return pdfFile;
    }

    private void sendEmailWithPdf(String email, String orderId, File pdfFile) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication("factoriostore@gmail.com", "vzmn oowl dvmq dhel");
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("factoriostore@gmail.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Ваш чек заказа Factorio #" + orderId);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Спасибо за заказ! Чек во вложении.");

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(pdfFile);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        Transport.send(message);
    }

    private int calculateTotalPrice(List<CartItem> items) {
        int totalPrice = 0;
        for (CartItem item : items) {
            totalPrice += item.getTotalPrice();
        }
        return totalPrice;
    }
}