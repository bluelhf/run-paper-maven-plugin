package blue.lhf.run_paper_maven_plugin;

import org.junit.jupiter.api.*;

public class InstallerTests {
    @Test
    public void roundtripTest() {
        final InstallMojo mojo = new InstallMojo();
        mojo.minecraftVersion = "1.19.3";
        Assertions.assertDoesNotThrow(mojo::execute);
    }
}
