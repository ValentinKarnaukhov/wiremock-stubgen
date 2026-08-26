package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collects imports while a source file is being written, and renders the block at the end.
 *
 * <p>The emitter names every type in full and asks this to shorten it. Deciding imports up
 * front would mean predicting which types the body will mention.
 */
final class Imports {

    private final Set<String> types = new TreeSet<>();

    private final Set<String> statics = new TreeSet<>();

    private final String ownPackage;

    Imports(String ownPackage) {
        this.ownPackage = ownPackage;
    }

    /**
     * Registers a type and hands back the name to write. Understands generics, so
     * {@code java.util.List<com.example.Body>} imports both and comes back as
     * {@code List<Body>}.
     *
     * <p>A nested type is written with a dollar between outer class and member, as
     * {@code com.example.Holder$MethodEnum}, and comes back as {@code Holder.MethodEnum}.
     * A dollar rather than a dot because only the caller knows where the package ends.
     */
    String use(String fullyQualified) {
        StringBuilder result = new StringBuilder();
        StringBuilder token = new StringBuilder();
        for (char c : fullyQualified.toCharArray()) {
            if (c == '<' || c == '>' || c == ',' || c == ' ') {
                result.append(shorten(token.toString()));
                token.setLength(0);
                result.append(c);
            } else {
                token.append(c);
            }
        }
        return result.append(shorten(token.toString())).toString();
    }

    /** Registers a static import and hands back the bare method name. */
    String useStatic(String fullyQualified) {
        statics.add(fullyQualified);
        return simpleName(fullyQualified);
    }

    private String shorten(String type) {
        int nested = type.indexOf('$');
        if (nested >= 0) {
            // The import names the outer class, never the member. Two schemas may each
            // declare an enum called StatusEnum, and importing both by their own name
            // would be ambiguous and not compile.
            return shorten(type.substring(0, nested)) + "." + type.substring(nested + 1).replace('$', '.');
        }
        if (type.isEmpty() || !type.contains(".")) {
            return type;
        }
        String packageName = type.substring(0, type.lastIndexOf('.'));
        // java.lang and the file's own package need no import, and some styles treat a
        // redundant java.lang import as an error.
        if (!packageName.equals("java.lang") && !packageName.equals(ownPackage)) {
            types.add(type);
        }
        return simpleName(type);
    }

    private static String simpleName(String fullyQualified) {
        return fullyQualified.substring(fullyQualified.lastIndexOf('.') + 1);
    }

    /**
     * The import block, in three groups: everything else, then {@code java.*}, then static.
     * The order IntelliJ produces by default, which is the order the goldens are in.
     */
    List<String> render() {
        List<String> lines = new java.util.ArrayList<>();
        List<String> other = types.stream().filter(t -> !t.startsWith("java.")).toList();
        List<String> java = types.stream().filter(t -> t.startsWith("java.")).toList();

        appendGroup(lines, other, "import ");
        appendGroup(lines, java, "import ");
        appendGroup(lines, List.copyOf(statics), "import static ");
        return lines;
    }

    private static void appendGroup(List<String> lines, List<String> group, String keyword) {
        if (group.isEmpty()) {
            return;
        }
        if (!lines.isEmpty()) {
            lines.add("");
        }
        group.forEach(type -> lines.add(keyword + type + ";"));
    }
}
