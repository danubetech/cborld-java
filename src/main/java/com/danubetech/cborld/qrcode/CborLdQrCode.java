package com.danubetech.cborld.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CborLdQrCode {
    public static final String FORMAT_PNG = "png";
    public static final String FORMAT_JPG = "jpg";

    public static final String DEFAULT_FORMAT = FORMAT_PNG;
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 400;

    public static byte[] toQrCode(byte[] bytes, String format, int width, int height) throws WriterException, IOException {
        String base32String = new Base32().encodeAsString(bytes);
        System.out.println(base32String);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(base32String, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ImageIO.write(bufferedImage, format, out);
        return out.toByteArray();
    }

    public static byte[] toQrCode(byte[] bytes, String format, int width) throws WriterException, IOException {
        return toQrCode(bytes, format, width, width);
    }

    public static byte[] toQrCode(byte[] bytes, String format) throws WriterException, IOException {
        return toQrCode(bytes, format, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static byte[] toQrCode(byte[] bytes, int width, int height) throws WriterException, IOException {
        return toQrCode(bytes, DEFAULT_FORMAT, width, height);
    }

    public static byte[] toQrCode(byte[] bytes, int width) throws WriterException, IOException {
        return toQrCode(bytes, DEFAULT_FORMAT, width, width);
    }

    public static byte[] toQrCode(byte[] bytes) throws WriterException, IOException {
        return toQrCode(bytes, DEFAULT_FORMAT, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}
