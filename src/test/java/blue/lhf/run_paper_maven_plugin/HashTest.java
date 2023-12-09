package blue.lhf.run_paper_maven_plugin;

import blue.lhf.run_paper_maven_plugin.util.Progressive;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("unused") // Used by Maven test runner
public class HashTest {
    public void testKnownHash() {
        final String plaintext = "titties";
        final String knownHash = "05b71b9303ad6c74e6237a2756c95f83ec887ed9cc9a2f71d6d66d60d392ec42";

        final InputStream input = new ByteArrayInputStream(plaintext.getBytes(UTF_8));
        try {
            final String result = InstallMojo.sha256(input, null);
            assert knownHash.equals(result): "SHA-256 hash computation is WRONG!";
        } catch (NoSuchAlgorithmException e) {
            assert false: "Can't test SHA-256 on non-compliant Java runtime with no SHA-256 implementation";
        } catch (IOException e) {
            assert false: "Not possible with ByteArrayInputStream";
        }
    }

    /*public void testLargeHash() throws IOException, NoSuchAlgorithmException {
        final URL resource = HashTest.class.getResource("/random_bytes");
        assert resource != null: "Cannot find random bytes";

        final URLConnection connection = resource.openConnection();
        final InputStream input = connection.getInputStream();
        final long length = connection.getContentLength();

        final String knownHash = "2fe1d860955cb79cff0f4b8bbfa0105119407b78401d395f1bc5b828a57b6a11";
        try (final Progressive progressive = Progressive.ofSize(length, "Computing hash...")) {
            final String result = InstallMojo.sha256(input, progressive::addProgress);
            assert result.equals(knownHash): "SHA-256 hash computation is WRONG!";
        }
    }*/
}
