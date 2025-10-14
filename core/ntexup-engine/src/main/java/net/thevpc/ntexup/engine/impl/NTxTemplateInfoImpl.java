package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPrimitiveElementBuilder;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NTxTemplateInfoImpl implements NTxTemplateInfo {
    private String id;
    private String name;
    private String layout;
    private String version;
    private String localPath;
    private String url;
    private String repoUrl;
    private String repoName;
    private String[] binaryVersions;
    private boolean recommended;

    public NTxTemplateInfoImpl(String id, String name, String layout, String version, String url, boolean recommended, String repoName, String repoUrl, String localPath, String[] binaryVersions) {
        this.id = id;
        this.name = name;
        this.localPath = localPath;
        this.repoUrl = repoUrl;
        this.repoName = repoName;
        this.url = url;
        this.recommended = recommended;
        this.binaryVersions = binaryVersions;
        this.layout = layout;
        this.version = version;
    }

    public String id() {
        if (id == null) {
            return NMsg.ofC("%s:%s-%s#%s", repoName, name, layout, version).toString();
        }
        return id;
    }

    @Override
    public NTxTemplateInfo withName(String name) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withRecommended(boolean recommended) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withBinaryVersions(String[] binaryVersions) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withLocalPath(String localPath) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withUrl(String url) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withRepoUrl(String repoUrl) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public NTxTemplateInfo withRepoName(String repoName) {
        return new NTxTemplateInfoImpl(id, name, layout, version, url, recommended, repoName, repoUrl, localPath, binaryVersions);
    }

    @Override
    public String repoUrl() {
        return repoUrl;
    }

    @Override
    public String repoName() {
        return repoName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String layout() {
        return layout;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String localPath() {
        return localPath;
    }

    public String url() {
        return url;
    }

    @Override
    public boolean recommended() {
        return recommended;
    }

    @Override
    public List<String> binaryVersions() {
        return Arrays.asList(binaryVersions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (NBlankable.isBlank(name)) {
            sb.append("<noname>");
        } else {
            sb.append(name.trim());
        }
        if (!NBlankable.isBlank(repoName)) {
            sb.append("(");
            sb.append(repoName.trim());
            sb.append(")");
        }
        return sb.toString();
    }


    @Override
    public NElement toElement() {
        NPrimitiveElementBuilder pb = NElement.ofPrimitiveBuilder();
        pb.setString(localPath);
        if (!NBlankable.isBlank(name)) {
            pb.addAnnotation("name", NElement.ofString(name));
        }
        if (recommended) {
            pb.addAnnotation("recommended");
        }
        if (binaryVersions.length > 0) {
            pb.addAnnotation("binaries", Stream.of(binaryVersions).map(NElement::ofString).toArray(NElement[]::new));
        }
        return pb.build();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        NTxTemplateInfoImpl that = (NTxTemplateInfoImpl) object;
        return recommended == that.recommended && Objects.equals(name, that.name) && Objects.equals(localPath, that.localPath) && Objects.equals(url, that.url) && Objects.equals(repoUrl, that.repoUrl) && Objects.equals(repoName, that.repoName) && Objects.deepEquals(binaryVersions, that.binaryVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, localPath, url, repoUrl, repoName, Arrays.hashCode(binaryVersions), recommended);
    }
}
