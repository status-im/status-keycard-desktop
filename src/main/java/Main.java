import im.status.hardwallet.lite.WalletAppletCommandSet;
import org.bouncycastle.util.encoders.Hex;

import javax.smartcardio.*;
import java.security.Security;

import static im.status.hardwallet.lite.WalletAppletCommandSet.checkOK;

public class Main {
  public static void main(String[] args) throws Exception {
    // we want to use BouncyCastle
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    // Find and a terminal
    TerminalFactory tf = TerminalFactory.getDefault();
    CardTerminal cardTerminal = null;

    for (CardTerminal t : tf.terminals().list()) {
      if (t.isCardPresent()) {
        cardTerminal = t;
        break;
      }
    }

    if (cardTerminal == null) {
      System.out.println("No card reader found, exiting.");
      return;
    }

    // Connect to the card and get the basic channel
    Card apduCard = cardTerminal.connect("*");
    CardChannel apduChannel = apduCard.getBasicChannel();

    // Applet-specific code
    WalletAppletCommandSet cmdSet = new WalletAppletCommandSet(apduChannel);

    // First thing to do is selecting the applet on the card.
    checkOK(cmdSet.select());

    // In real projects, the pairing key should be saved and used for all new sessions.
    cmdSet.autoPair("WalletAppletTest");

    // Opening a Secure Channel is needed for all other applet commands
    cmdSet.autoOpenSecureChannel();

    // We send a GET STATUS command, which does not require PIN authentication
    ResponseAPDU resp = checkOK(cmdSet.getStatus(WalletAppletCommandSet.GET_STATUS_P1_APPLICATION));
    System.out.println("GET STATUS response: " + Hex.toHexString(resp.getData()));

    // PIN authentication allows execution of privileged commands
    cmdSet.verifyPIN("000000");

    // Cleanup, in a real application you would not unpair and instead keep the pairing key for successive interactions.
    // We also remove all other pairings so that we do not fill all slots with failing runs. Again in real application
    // this would be a very bad idea to do.
    cmdSet.unpairOthers();
    cmdSet.autoUnpair();
  }
}
