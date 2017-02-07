package mbc.util

import mbc.core.Block
import mbc.core.Transaction
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.nio.ByteBuffer
import java.security.*
import java.security.Security.insertProviderAt
import java.security.spec.ECGenParameterSpec

/**
 * 密码学工具类。
 */
class CryptoUtil {

  companion object {
    init {
      insertProviderAt(BouncyCastleProvider(), 1)
    }

    /**
     * 根据公钥(public key)推算出账户地址，使用以太坊的算法，先KECCAK-256计算哈希值(32位)，取后20位作为账户地址。
     * 比特币地址算法：http://www.infoq.com/cn/articles/bitcoin-and-block-chain-part03
     * 以太坊地址算法：http://ethereum.stackexchange.com/questions/3542/how-are-ethereum-addresses-generated
     */
    fun generateAddress(publicKey: PublicKey): String {
      val digest = MessageDigest.getInstance("KECCAK-256", "SC")
      digest.update(publicKey.encoded)
      val hash = digest.digest()

      return Hex.toHexString(hash.drop(12).toByteArray())
    }

    /**
     * 生成公私钥对，使用以太坊的ECDSA算法(secp256k1)。
     */
    fun generateKeyPair(): KeyPair? {
      val gen = KeyPairGenerator.getInstance("EC", "SC")
      gen.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
      val keyPair = gen.generateKeyPair()
      return keyPair
    }

    /**
     * 发送方用私钥对交易Transaction进行签名。
     */
    fun signTransaction(trx: Transaction, privateKey: PrivateKey): ByteArray {
      val signer = Signature.getInstance("SHA256withECDSA")
      signer.initSign(privateKey)
      val msgToSign = encodeTransaction(trx)
      signer.update(msgToSign)
      return signer.sign()
    }

    /**
     * 验证交易Transaction签名的有效性。
     */
    fun verifyTransactionSignature(trx: Transaction, signature: ByteArray): Boolean {
      val signer = Signature.getInstance("SHA256withECDSA")
      signer.initVerify(trx.publicKey)

      signer.update(encodeTransaction(trx))
      return signer.verify(signature)
    }

    /**
     * 运算区块的哈希值。
     */
    fun hashBlock(block: Block): ByteArray {
      val digest = MessageDigest.getInstance("KECCAK-256", "SC")
      digest.update(encodeBlock(block))
      return digest.digest()
    }

    /**
     * 序列化交易(Transaction)。当前实现非常简单，后期会改成以太坊的RLP协议。
     */
    private fun encodeTransaction(
        trx: Transaction) = (trx.senderAddress + trx.receiverAddress + trx.amount.toString() + trx.time.millis.toString()).toByteArray()

    /**
     * 序列化区块(Block)。当前实现非常简单，后期会改成以太坊的RLP协议。
     */
    private fun encodeBlock(block: Block): ByteArray {
      val byteBuffer = ByteBuffer.allocate(1024)
      byteBuffer.put(block.minerAddress.toByteArray())
      byteBuffer.put(block.time.millis.toString().toByteArray())
      block.transactions.map { byteBuffer.put(encodeTransaction(it)) }

      byteBuffer.flip()
      return byteBuffer.array()
    }

  }

}
