package net.thevpc.ntexup.config;

import net.thevpc.nuts.util.NStringUtils;

import java.util.Objects;

public class UserConfig implements Cloneable {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String emailAddress;
    private String affiliation;

    public String getId() {
        return id;
    }

    public UserConfig setId(String id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String resolveFullName() {
        return NStringUtils.firstNonBlankTrimmed(
                getFullName(),
                NStringUtils.trimToNull(NStringUtils.trim(getFirstName()) + " " + NStringUtils.trim(getLastName())),
                System.getProperty("user.name")
        );
    }

    public String getFirstName() {
        return firstName;
    }

    public UserConfig setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserConfig setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserConfig setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public UserConfig setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public UserConfig setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConfig that = (UserConfig) o;
        return Objects.equals(fullName, that.fullName) && Objects.equals(emailAddress, that.emailAddress) && Objects.equals(affiliation, that.affiliation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, emailAddress, affiliation);
    }

    @Override
    public String toString() {
        return "UserConfig{" +
                "author='" + fullName + '\'' +
                ", email='" + emailAddress + '\'' +
                ", affiliation='" + affiliation + '\'' +
                '}';
    }

    @Override
    protected UserConfig clone() {
        try {
            return (UserConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public UserConfig copy() {
        return clone();
    }


    public UserConfig mergeNonBlankOther(UserConfig other) {
        this.setId(NStringUtils.firstNonBlank(other.getId(), this.getId()));
        this.setUsername(NStringUtils.firstNonBlank(other.getUsername(), this.getUsername()));
        this.setFirstName(NStringUtils.firstNonBlank(other.getFirstName(), this.getFirstName()));
        this.setLastName(NStringUtils.firstNonBlank(other.getLastName(), this.getLastName()));
        this.setFullName(NStringUtils.firstNonBlank(other.getFullName(), this.getFullName()));
        this.setAffiliation(NStringUtils.firstNonBlank(other.getAffiliation(), this.getAffiliation()));
        this.setEmailAddress(NStringUtils.firstNonBlank(other.getEmailAddress(), this.getEmailAddress()));
        return this;
    }

    public UserConfig mergeBlankSelf(UserConfig other) {
        this.setId(NStringUtils.firstNonBlank(this.getId(), other.getId()));
        this.setUsername(NStringUtils.firstNonBlank(this.getUsername(), other.getUsername()));
        this.setFirstName(NStringUtils.firstNonBlank(this.getFirstName(), other.getFirstName()));
        this.setLastName(NStringUtils.firstNonBlank(this.getLastName(), other.getLastName()));
        this.setFullName(NStringUtils.firstNonBlank(this.getFullName(), other.getFullName()));
        this.setAffiliation(NStringUtils.firstNonBlank(this.getAffiliation(), other.getAffiliation()));
        this.setEmailAddress(NStringUtils.firstNonBlank(this.getEmailAddress(), other.getEmailAddress()));
        return this;
    }

    public UserConfig mergeAlways(UserConfig other) {
        this.setId(other.getId());
        this.setUsername(other.getUsername());
        this.setFirstName(other.getFirstName());
        this.setLastName(other.getLastName());
        this.setFullName(other.getFullName());
        this.setAffiliation(other.getAffiliation());
        this.setEmailAddress(other.getEmailAddress());
        return this;
    }

    public UserConfig validate() {
        String id = NStringUtils.firstNonBlank(NStringUtils.trimToNull(this.getId()),"default");
        this.setId(id);
        this.setAffiliation(NStringUtils.trimToNull(this.getAffiliation()));
        this.setEmailAddress(NStringUtils.trimToNull(this.getEmailAddress()));
        this.setUsername(NStringUtils.trimToNull(this.getUsername()));
        this.setLastName(NStringUtils.trimToNull(this.getLastName()));
        this.setFullName(NStringUtils.trimToNull(this.getFullName()));
        this.setFirstName(NStringUtils.trimToNull(this.getFirstName()));
        return this;
    }

}
