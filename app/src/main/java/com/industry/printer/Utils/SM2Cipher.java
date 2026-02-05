package com.industry.printer.Utils;

import com.industry.printer.Server1.Server1MainWindow;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SM2Cipher {
    public static final String TAG = SM2Cipher.class.getSimpleName();

    private static final String HEX_PUBLIC_KEY_STRING = "04a23028f204ecd1d418ad994cfde98c8e1cefb88f51d454a779ff7eb4cab0f32dd41daa6967f450e1c449d08c99718bebe987a51586cf067db6d76f8ac68e9321";
    private static ECPublicKeyParameters SM2_PUBLIC_KEY_PARAM = null;

    static {
        Security.addProvider(new BouncyCastleProvider());
        double version = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME).getVersion();
        Debug.i(TAG, "Server1.0: " + version);
    }

    public static ECPublicKeyParameters createSM2PublicKeyFromHex(String hexPublicKeyString) {
        if(null != SM2_PUBLIC_KEY_PARAM) return SM2_PUBLIC_KEY_PARAM;

        // 获取SM2曲线参数
        X9ECParameters sm2ECParameters = GMNamedCurves.getByName("sm2p256v1");
        ECDomainParameters domainParameters = new ECDomainParameters(
            sm2ECParameters.getCurve(),
            sm2ECParameters.getG(),
            sm2ECParameters.getN(),
            sm2ECParameters.getH()
        );

        // 解析公钥字节，0x04表示未压缩格式
        byte[] publicKeyBytes = Hex.decode(hexPublicKeyString);

        // 验证公钥格式
        if(publicKeyBytes.length != 65 || publicKeyBytes[0] != 0x04) {
            return null;
        }

        ECPoint ecPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(hexPublicKeyString));

        // 创建公钥参数
        SM2_PUBLIC_KEY_PARAM = new ECPublicKeyParameters(ecPoint, domainParameters);
        return SM2_PUBLIC_KEY_PARAM;
    }

    public static byte[] encrypt(byte[] plainBytes) {
        try {
            createSM2PublicKeyFromHex(HEX_PUBLIC_KEY_STRING);
            SM2Engine sm2Engine = new SM2Engine(new SM3Digest(), SM2Engine.Mode.C1C3C2);
            sm2Engine.init(true, new ParametersWithRandom(SM2_PUBLIC_KEY_PARAM, new SecureRandom()));
            return sm2Engine.processBlock(plainBytes, 0, plainBytes.length);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
