const functions = require("firebase-functions");
const admin = require("firebase-admin");
const PDFDocument = require("pdfkit");
const nodemailer = require("nodemailer");

admin.initializeApp();

// Настройка Nodemailer (используем Gmail как пример)
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "factoriostore@gmail.com",
    pass: "vzmn oowl dvmq dhel",
  },
});

// Триггер на создание нового заказа в Firestore
exports.sendOrderReceipt = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const orderData = snap.data();
    const orderId = context.params.orderId;
    const totalPrice = orderData.totalPrice;
    const items = orderData.items;
    const email = orderData.email;

    const doc = new PDFDocument();
    const buffers = [];
    doc.on("data", buffers.push.bind(buffers));
    doc.on("end", async () => {
      const pdfBuffer = Buffer.concat(buffers);

      // Формирование чека в PDF
      doc.fontSize(16).text("Чек заказа", {align: "center"});
      doc.moveDown();
      doc.fontSize(12).text(`Заказ #${orderId}`);
      doc.text(`Дата: ${new Date().toLocaleString()}`);
      doc.text(`Пользователь: ${email}`);
      doc.moveDown();
      doc.text("Товары:", {underline: true});
      items.forEach((item) => {
        doc.text(
          `${item.name} - ${item.quantity} x ${item.price} руб. = ${
            item.quantity * item.price
          } руб.`
        );
      });
      doc.moveDown();
      doc.text(`Итого: ${totalPrice} руб.`, {align: "right"});
      doc.end();

      const mailOptions = {
        from: "factoriostore@gmail.com",
        to: email,
        subject: `Ваш чек заказа #${orderId}`,
        text: "Спасибо за заказ! Чек во вложении.",
        attachments: [
          {
            filename: `receipt_${orderId}.pdf`,
            content: pdfBuffer,
            contentType: "application/pdf",
          },
        ],
      };

      try {
        await transporter.sendMail(mailOptions);
        console.log(`Чек отправлен на ${email}`);
      } catch (error) {
        console.error("Ошибка отправки email:", error);
      }
    });

    doc.end();
    return null;
  });
