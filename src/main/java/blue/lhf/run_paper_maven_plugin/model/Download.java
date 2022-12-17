package blue.lhf.run_paper_maven_plugin.model;

import java.io.InputStream;
import java.util.*;

/**
 * An input stream paired with a hash and length, if known.
 * */
public record Download(InputStream stream, OptionalLong length, Optional<String> sha256) { }
