package fr.ans.psc.model;

import java.util.ArrayList;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Professionnel de santé
 */
@Document(collection = "ps")
@ApiModel(description = "Professionnel de santé")
public class Ps {

    @Id
    private String _id;

    @JsonProperty("idType")
    private String idType;

    @JsonProperty("id")
    private String id;

    @JsonProperty("nationalId")
    @Indexed(unique = true)
    @NotNull(message = "nationalId should not be null")
    private String nationalId;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("dateOfBirth")
    private String dateOfBirth;

    @JsonProperty("birthAddressCode")
    private String birthAddressCode;

    @JsonProperty("birthCountryCode")
    private String birthCountryCode;

    @JsonProperty("birthAddress")
    private String birthAddress;

    @JsonProperty("genderCode")
    private String genderCode;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("salutationCode")
    private String salutationCode;

    @JsonProperty("professions")
    @Valid
    private List<Profession> professions = null;

    @JsonProperty("otherIds")
    private List<String> otherIds = null;


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * Get idType
     *
     * @return idType
     */
    @ApiModelProperty(value = "")
    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    /**
     * Get id
     *
     * @return id
     */
    @ApiModelProperty(value = "")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get nationalId
     *
     * @return nationalId
     */
    @ApiModelProperty(required = true, value = "")
    @NotNull
    @Size(min = 1)
    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    /**
     * Get lastName
     *
     * @return lastName
     */
    @ApiModelProperty(value = "")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get firstName
     *
     * @return firstName
     */
    @ApiModelProperty(value = "")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Get dateOfBirth
     *
     * @return dateOfBirth
     */
    @ApiModelProperty(value = "")
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Get birthAddressCode
     *
     * @return birthAddressCode
     */
    @ApiModelProperty(value = "")
    public String getBirthAddressCode() {
        return birthAddressCode;
    }

    public void setBirthAddressCode(String birthAddressCode) {
        this.birthAddressCode = birthAddressCode;
    }

    /**
     * Get birthCountryCode
     *
     * @return birthCountryCode
     */
    @ApiModelProperty(value = "")
    public String getBirthCountryCode() {
        return birthCountryCode;
    }

    public void setBirthCountryCode(String birthCountryCode) {
        this.birthCountryCode = birthCountryCode;
    }

    /**
     * Get birthAddress
     *
     * @return birthAddress
     */
    @ApiModelProperty(value = "")
    public String getBirthAddress() {
        return birthAddress;
    }

    public void setBirthAddress(String birthAddress) {
        this.birthAddress = birthAddress;
    }

    /**
     * Get genderCode
     *
     * @return genderCode
     */
    @ApiModelProperty(value = "")
    public String getGenderCode() {
        return genderCode;
    }

    public void setGenderCode(String genderCode) {
        this.genderCode = genderCode;
    }

    /**
     * Get phone
     *
     * @return phone
     */
    @ApiModelProperty(value = "")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Get email
     *
     * @return email
     */
    @ApiModelProperty(value = "")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get salutationCode
     *
     * @return salutationCode
     */
    @ApiModelProperty(value = "")
    public String getSalutationCode() {
        return salutationCode;
    }

    public void setSalutationCode(String salutationCode) {
        this.salutationCode = salutationCode;
    }

    /**
     * Get professions
     *
     * @return professions
     */
    @ApiModelProperty(value = "")
    @Valid
    public List<Profession> getProfessions() {
        return professions;
    }

    public void setProfessions(List<Profession> professions) {
        this.professions = professions;
    }

    public List<String> getOtherIds() {
        return otherIds;
    }

    public void setOtherIds(List<String> otherIds) {
        this.otherIds = otherIds;
    }

    public void extractOtherIds(List<PsRef> psRefs) {
        List<String> otherIds = new ArrayList<>();
        psRefs.forEach(psRef -> otherIds.add(psRef.getNationalIdRef()));
        this.otherIds = otherIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ps ps = (Ps) o;
        return Objects.equals(this.idType, ps.idType) &&
                Objects.equals(this.id, ps.id) &&
                Objects.equals(this.nationalId, ps.nationalId) &&
                Objects.equals(this.lastName, ps.lastName) &&
                Objects.equals(this.firstName, ps.firstName) &&
                Objects.equals(this.dateOfBirth, ps.dateOfBirth) &&
                Objects.equals(this.birthAddressCode, ps.birthAddressCode) &&
                Objects.equals(this.birthCountryCode, ps.birthCountryCode) &&
                Objects.equals(this.birthAddress, ps.birthAddress) &&
                Objects.equals(this.genderCode, ps.genderCode) &&
                Objects.equals(this.phone, ps.phone) &&
                Objects.equals(this.email, ps.email) &&
                Objects.equals(this.salutationCode, ps.salutationCode) &&
                Objects.equals(this.professions, ps.professions) &&
                Objects.equals(this.otherIds, ps.otherIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idType, id, nationalId, lastName, firstName, dateOfBirth, birthAddressCode, birthCountryCode, birthAddress, genderCode, phone, email, salutationCode, professions, otherIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Ps {\n");

        sb.append("    idType: ").append(toIndentedString(idType)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    nationalId: ").append(toIndentedString(nationalId)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    birthAddressCode: ").append(toIndentedString(birthAddressCode)).append("\n");
        sb.append("    birthCountryCode: ").append(toIndentedString(birthCountryCode)).append("\n");
        sb.append("    birthAddress: ").append(toIndentedString(birthAddress)).append("\n");
        sb.append("    genderCode: ").append(toIndentedString(genderCode)).append("\n");
        sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    salutationCode: ").append(toIndentedString(salutationCode)).append("\n");
        sb.append("    professions: ").append(toIndentedString(professions)).append("\n");
        sb.append("    otherIds: ").append(toIndentedString(otherIds)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

