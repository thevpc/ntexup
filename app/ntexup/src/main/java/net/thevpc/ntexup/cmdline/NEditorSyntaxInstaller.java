package net.thevpc.ntexup.cmdline;

import net.thevpc.nuts.artifact.NVersion;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NCopiable;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.util.NStringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;

public class NEditorSyntaxInstaller {
    Info info;

    public static class Info implements NCopiable, Cloneable {
        private String langId;
        private String repoFolder = "https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration";
        private String fileExtension = "ntx";
        private boolean idea;
        private boolean kate;
        private boolean gedit;
        private boolean jedit;
        private boolean vim;
        private boolean vscode;
        private boolean notepadPlusPlus;
        private boolean force;


        public String getFileExtension() {
            return fileExtension;
        }

        public Info setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        public String getLangId() {
            return langId;
        }

        public Info setLangId(String langId) {
            this.langId = langId;
            return this;
        }

        public String getRepoFolder() {
            return repoFolder;
        }

        public Info setRepoFolder(String repoFolder) {
            this.repoFolder = repoFolder;
            return this;
        }

        public boolean isIdea() {
            return idea;
        }

        public Info setIdea(boolean idea) {
            this.idea = idea;
            return this;
        }

        public boolean isKate() {
            return kate;
        }

        public Info setKate(boolean kate) {
            this.kate = kate;
            return this;
        }

        public boolean isGedit() {
            return gedit;
        }

        public Info setGedit(boolean gedit) {
            this.gedit = gedit;
            return this;
        }

        public boolean isJedit() {
            return jedit;
        }

        public Info setJedit(boolean jedit) {
            this.jedit = jedit;
            return this;
        }

        public boolean isVim() {
            return vim;
        }

        public Info setVim(boolean vim) {
            this.vim = vim;
            return this;
        }

        public boolean isVscode() {
            return vscode;
        }

        public Info setVscode(boolean vscode) {
            this.vscode = vscode;
            return this;
        }

        public boolean isNotepadPlusPlus() {
            return notepadPlusPlus;
        }

        public Info setNotepadPlusPlus(boolean notepadPlusPlus) {
            this.notepadPlusPlus = notepadPlusPlus;
            return this;
        }

        public boolean isForce() {
            return force;
        }

        public Info setForce(boolean force) {
            this.force = force;
            return this;
        }

        public Info addEditor(String name) {
            for (String s : NStringUtils.split(NStringUtils.trim(name).toLowerCase(), ",;:|")) {
                switch (s) {
                    case "idea":
                    case "intellij": {
                        setIdea(true);
                        break;
                    }
                    case "kate": {
                        setKate(true);
                        break;
                    }
                    case "gedit": {
                        setGedit(true);
                        break;
                    }
                    case "vi":
                    case "vim": {
                        setVim(true);
                        break;
                    }
                    case "jedit": {
                        setJedit(true);
                        break;
                    }
                    case "notepad++":
                    case "notepadpp":
                    case "notepadplusplus":
                    case "notepad-plus-plus": {
                        setNotepadPlusPlus(true);
                        break;
                    }
                    case "vscode": {
                        setVscode(true);
                        break;
                    }
                    case "all": {
                        setVim(true);
                        setIdea(true);
                        setGedit(true);
                        setKate(true);
                        setJedit(true);
                        setNotepadPlusPlus(true);
                        setVscode(true);
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("unknown editor: " + name);
                    }
                }
            }
            return this;
        }

        @Override
        public Info copy() {
            return clone();
        }

        @Override
        public Info clone() {
            try {
                return (Info) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public NEditorSyntaxInstaller(Info info) {
        this.info = info;
    }

    public void run() {
        if (info.isIdea()) {
            runActionInstallIdea();
        }
        if (info.isKate()) {
            runActionInstallKate();
        }
        if (info.isVim()) {
            runActionInstallVim();
        }
        if (info.isGedit()) {
            runActionInstallGedit();
        }
        if (info.isJedit()) {
            runActionInstallJEdit();
        }
        if (info.isNotepadPlusPlus()) {
            runActionInstallNodepadPlusPlus();
        }
        if (info.isVscode()) {
            runActionInstallVscode();
        }
    }

    private void runActionInstallIdea() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("IntelliJIdea");
        NPath local = null;
        // ".config/JetBrains/IntelliJIdea2024.1/filetypes/
        NPath jb = NPath.ofUserHome().resolve(".config/JetBrains/");
        boolean doForce = false;
        if (jb.isDirectory()) {
            local = jb.list().stream().filter(x -> {
                if (!x.isDirectory()) {
                    return false;
                }
                if (!x.resolve("idea64.vmoptions").isRegularFile()) {
                    return false;
                }
                return true;
            }).sorted((a, b) -> {
                return -NVersion.of(resolveJetbrainsVersion(a.getName()))
                        .compareTo(resolveJetbrainsVersion(b.getName()));
            }).findFirst().orElse(null);
            if (local != null) {
                local = local.resolve("filetypes").resolve(info.getLangId() + ".xml");
            }
        }
        if (local == null) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported. Idea does not seem to be installed", styledLangId, app, NWorkspace.of().getOsFamily()));
            return;
        } else if (!local.isRegularFile()) {
        } else {
            if (info.isForce()) {
                doForce = true;
            } else {
                NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                return;
            }
        }
        NPath remote = NPath.of(info.getRepoFolder()).resolve("/intellij/" + info.getLangId() + ".xml");
        remote.copyTo(local.mkParentDirs());
        if (doForce) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
        }
    }

    private String resolveJetbrainsVersion(String name) {
        if (name.startsWith("IdeaIC")) {
            return name.substring("IdeaIC".length());
        }
        if (name.startsWith("IntelliJIdea")) {
            return name.substring("IntelliJIdea".length());
        }
        return "";
    }

    private void runActionInstallKate() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("kate");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".local/share/org.kde.syntax-highlighting/syntax/" + info.getLangId() + ".xml");
            boolean doForce = false;
            if (!local.isRegularFile()) {
            } else {
                if (info.isForce()) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                    return;
                }
            }
            NPath remote = NPath.of(info.getRepoFolder()).resolve("/kate/" + info.getLangId() + ".xml");
            remote.copyTo(local.mkParentDirs());
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", styledLangId, app, local));
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            }

        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", styledLangId, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallVim() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("vim");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".vim/");
            boolean doForce = false;
            if (
                    !local.resolve("syntax/" + info.getLangId() + ".vim").isRegularFile()
                            && !local.resolve("ftdetect/" + info.getLangId() + ".vim").isRegularFile()
            ) {
                //
            } else {
                if (info.isForce()) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                    return;
                }
            }

            NPath remote = NPath.of(info.getRepoFolder()).resolve("/vim/syntax/" + info.getLangId() + ".vim");
            remote.copyTo(local.resolve("syntax/" + info.getLangId() + ".vim").mkParentDirs());
            remote = NPath.of(info.getRepoFolder()).resolve("/vim/ftdetect/" + info.getLangId() + ".vim");
            remote.copyTo(local.resolve("ftdetect/" + info.getLangId() + ".vim").mkParentDirs());

            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            }

        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", styledLangId, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallVscode() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("Visual Studio Code");
        String pluginName = info.getLangId() + "-syntax";
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".vscode/extensions/" + pluginName);
            boolean doForce = false;
            if (
                    !local.isDirectory()
                            || !local.resolve("package.json").isRegularFile()
            ) {
                //
            } else {
                if (info.isForce()) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                    return;
                }
            }

            NPath remote = NPath.of(info.getRepoFolder()).resolve("/vscode/" + pluginName + "/package.json");
            remote.copyTo(local.resolve("package.json").mkParentDirs());
            remote = NPath.of(info.getRepoFolder()).resolve("/vscode/" + pluginName + "/syntaxes/" + info.getLangId() + ".tmLanguage.json");
            remote.copyTo(local.resolve("syntaxes/" + info.getLangId() + ".tmLanguage.json").mkParentDirs());

            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            }

        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", styledLangId, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallGedit() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("gedit");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".local/share/gtksourceview-4/language-specs/" + info.getLangId() + ".lang");
            boolean doForce = false;
            if (!local.isRegularFile()) {
                //
            } else {
                if (info.isForce()) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                    return;
                }
            }

            NPath remote = NPath.of(info.getRepoFolder()).resolve("/gedit/" + info.getLangId() + ".lang");
            remote.copyTo(local.mkParentDirs());
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            }

        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", styledLangId, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallNodepadPlusPlus() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("Nodepad++");
        if (NWorkspace.of().getOsFamily().isWindow()) {
            NPath local = NPath.ofUserHome().resolve(NWorkspace.of().getSysEnv("APPDATA") + "/Notepad++/userDefineLangs/" + info.getLangId() + ".xml");
            boolean doForce = false;
            if (!local.isRegularFile()) {
                //
            } else {
                if (info.isForce()) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                    return;
                }
            }

            NPath remote = NPath.of(info.getRepoFolder()).resolve("/notepad-plus-plus/" + info.getLangId() + ".xml");
            remote.copyTo(local.mkParentDirs());
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", styledLangId, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallJEdit() {
        NMsg styledLangId = NMsg.ofStyledKeyword(info.getLangId());
        NMsg app = NMsg.ofStyledDate("jEdit");
        NPath local = NPath.ofUserHome().resolve(".jedit/modes/" + info.getLangId() + ".xml");
        boolean doForce = false;
        if (!local.isRegularFile()) {
            //
        } else {
            if (info.isForce()) {
                doForce = true;
            } else {
                NOut.println(NMsg.ofC("Skipped installation : %s %s syntax highlighting already supported as %s", styledLangId, app, local));
                return;
            }
        }

        NPath remote = NPath.of(info.getRepoFolder()).resolve("/jedit/" + info.getLangId() + ".xml");
        remote.copyTo(local.mkParentDirs());

        // update catalog
        try {
            // Ensure parent directory exists
            NPath catalog = local.resolveSibling("catalog");

            Document doc;
            Element modesElement;

            if (catalog.isRegularFile()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);

                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(catalog.toFile().get());

                modesElement = doc.getDocumentElement();

                // Check if mode already exists
                if (runActionInstallJEdit_modeExists(modesElement, info.getLangId())) {
//                    System.out.println("Mode '" + info.getLangId() + "' already exists in catalog.");
                    return;
                }
            } else {
//                System.out.println("Creating new catalog file...");
                doc = createNewCatalog();
                modesElement = doc.getDocumentElement();
            }

            // Add the new mode
            runActionInstallJEdit_addMode(doc, modesElement, info.getLangId(), info.getLangId()+".xml", "*."+info.getFileExtension());

            // Write back to file
            runActionInstallJEdit_writeCatalog(doc, catalog);

            //System.out.println("Successfully updated catalog: " + catalogPath);
        } catch (TransformerException e) {
            throw new NIllegalArgumentException(NMsg.ofC("error : ", e), e);
        } catch (ParserConfigurationException e) {
            throw new NIllegalArgumentException(NMsg.ofC("error : ", e), e);
        } catch (IOException e) {
            throw new NIllegalArgumentException(NMsg.ofC("error : ", e), e);
        } catch (SAXException e) {
            throw new NIllegalArgumentException(NMsg.ofC("error : ", e), e);
        }

        if (doForce) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s. You might need to restart %s", styledLangId, app, local, app));
        }
    }

    /**
     * Creates a new catalog document from scratch.
     */
    private static Document createNewCatalog() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Add DOCTYPE
        DOMImplementation domImpl = doc.getImplementation();
        DocumentType doctype = domImpl.createDocumentType("MODES",
                null, "catalog.dtd");

        // Create root element
        Element modesElement = doc.createElement("MODES");
        doc.appendChild(modesElement);

        return doc;
    }

    private static boolean runActionInstallJEdit_modeExists(Element modesElement, String modeName) {
        NodeList modeNodes = modesElement.getElementsByTagName("MODE");
        for (int i = 0; i < modeNodes.getLength(); i++) {
            Element modeElement = (Element) modeNodes.item(i);
            String name = modeElement.getAttribute("NAME");
            if (modeName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new MODE element to the catalog.
     */
    private static void runActionInstallJEdit_addMode(Document doc, Element modesElement,
                                                      String name, String file, String fileGlob) {
        Element modeElement = doc.createElement("MODE");
        modeElement.setAttribute("NAME", name);
        modeElement.setAttribute("FILE", file);
        modeElement.setAttribute("FILE_NAME_GLOB", fileGlob);

        // Add with proper indentation
        Text indent = doc.createTextNode("\n  ");
        modesElement.appendChild(indent);
        modesElement.appendChild(modeElement);

        // Add final newline before closing tag
        Text finalNewline = doc.createTextNode("\n");
        modesElement.appendChild(finalNewline);
    }

    /**
     * Writes the document back to the catalog file with proper formatting.
     */
    private static void runActionInstallJEdit_writeCatalog(Document doc, NPath catalogPath) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set output properties for nice formatting
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "catalog.dtd");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(catalogPath.toFile().get());
        transformer.transform(source, result);
    }
}
