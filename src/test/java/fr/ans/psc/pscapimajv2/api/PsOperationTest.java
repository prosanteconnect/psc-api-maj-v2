package fr.ans.psc.pscapimajv2.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.LoggerContext;
import com.jupiter.tools.spring.test.mongo.annotation.ExpectedMongoDataSet;
import com.jupiter.tools.spring.test.mongo.annotation.MongoDataSet;
import fr.ans.psc.delegate.PsApiDelegateImpl;
import fr.ans.psc.model.Ps;
import fr.ans.psc.model.PsRef;
import fr.ans.psc.repository.PsRefRepository;
import fr.ans.psc.repository.PsRepository;
import fr.ans.psc.utils.ApiUtils;
import fr.ans.psc.pscapimajv2.utils.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class PsOperationTest extends BaseOperationTest {

    @Autowired
    private PsRepository psRepository;
    @Autowired
    private PsRefRepository psRefRepository;

    @BeforeEach
    public void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocProvider) {
        // LOG APPENDER
        Logger logger = (Logger) LoggerFactory.getLogger(PsApiDelegateImpl.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();

        // REST DOCS
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(documentationConfiguration(restDocProvider))
                .build();
    }

    @Test
    @DisplayName(value = "should get Ps by id, nominal case")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void getPsById() throws Exception {

        Ps storedPs = psRepository.findByNationalId("800000000001");
        storedPs.extractOtherIds(psRefRepository.findAllByNationalId("800000000001"));

        String psAsJsonString = objectWriter.writeValueAsString(storedPs);

        ResultActions firstPsRefRequest = mockMvc.perform(get("/api/v2/ps/800000000001")
                .header("Accept", "application/json"))
                .andExpect(status().is(200));

        firstPsRefRequest.andExpect(content().json(psAsJsonString));
        assertThat(memoryAppender.contains("Ps 800000000001 has been found", Level.INFO)).isTrue();

        firstPsRefRequest.andDo(document("PsOperationTest/get_Ps_by_id"));

        ResultActions secondPsRefRequest = mockMvc.perform(get("/api/v2/ps/800000000011")
                .header("Accept", "application/json"))
                .andExpect(status().is(200));

        secondPsRefRequest.andExpect(content().json(psAsJsonString));
        assertThat(memoryAppender.contains("Ps 800000000001 has been found", Level.INFO)).isTrue();

    }

    @Test
    @DisplayName(value = "should get all PsRef by id, nominal case")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void getAllPsRefs() {
        List<PsRef> allPsRefs = psRefRepository.findAllByNationalId("800000000001");
        assertEquals(2, allPsRefs.size());
    }

    @Test
    @DisplayName("check encoded url")
    @MongoDataSet(value = "/dataset/psEncodedId.json", cleanBefore = true, cleanAfter = true)
    public void getPsByEncodedId() throws Exception {
        Ps storedPs = psRepository.findByNationalId("80000000000/1");
        storedPs.extractOtherIds(psRefRepository.findAllByNationalId("80000000000/1"));
        String psAsJsonString = objectWriter.writeValueAsString(storedPs);
        ResultActions psRefRequest = mockMvc.perform(get("/api/v2/ps/80000000000%2F1")
                .header("Accept", "application/json"))
                .andExpect(status().is(200))
                .andDo(print());

        psRefRequest.andExpect(content().json(psAsJsonString));
    }

    @Test
    @DisplayName(value = "should get Ps if missing header")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void getPsWithoutJsonAcceptHeader() throws Exception {
        mockMvc.perform(get("/api/v2/ps/800000000001"))
                .andExpect(status().is(200));
    }

    @Test
    @DisplayName(value = "should not get Ps if wrong accept header")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void getPsWithWrongHeaderFailed() throws Exception {
        mockMvc.perform(get("/api/v2/ps/800000000001").header("Accept","application/xml"))
                .andExpect(status().is(406));
    }

    @Test
    @DisplayName(value = "should not get Ps if deactivated")
    @MongoDataSet(value = "/dataset/deactivated_ps.json", cleanBefore = true, cleanAfter = true)
    public void getPsDeactivated() throws Exception {
        mockMvc.perform(get("/api/v2/ps/800000000002")
                .header("Accept", "application/json"))
                .andExpect(status().is(410));
        assertThat(memoryAppender.contains("Ps 800000000002 is deactivated", Level.WARN)).isTrue();
    }

    @Test
    @DisplayName(value = "should not get Ps if not exist")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void getNotExistingPs() throws Exception {
        mockMvc.perform(get("/api/v2/ps/800000000003")
                .header("Accept", "application/json"))
                .andExpect(status().is(410));
        assertThat(memoryAppender.contains("No Ps found with nationalIdRef 800000000003", Level.WARN)).isTrue();
    }

    @Test
    @DisplayName(value = "should create a brand new Ps")
    public void createNewPs() throws Exception {

        ResultActions createdPs = mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\"idType\":\"8\",\"id\":\"00000000001\"," +
                        "\"nationalId\":\"800000000001\",\"lastName\":\"DUPONT\",\"firstName\":\"JIMMY''\",\"dateOfBirth\":\"17/12/1983\"," +
                        "\"birthAddressCode\":\"57463\",\"birthCountryCode\":\"99000\",\"birthAddress\":\"METZ\",\"genderCode\":\"M\"," +
                        "\"phone\":\"0601020304\",\"email\":\"toto57@hotmail.fr\",\"salutationCode\":\"MME\",\"professions\":[{\"exProId\":\"50C\"," +
                        "\"code\":\"50\",\"categoryCode\":\"C\",\"salutationCode\":\"M\",\"lastName\":\"DUPONT\",\"firstName\":\"JIMMY\"," +
                        "\"expertises\":[{\"expertiseId\":\"SSM69\",\"typeCode\":\"S\",\"code\":\"SM69\"}],\"workSituations\":[{\"situId\":\"SSA04\"," +
                        "\"modeCode\":\"S\",\"activitySectorCode\":\"SA04\",\"pharmacistTableSectionCode\":\"AC36\",\"roleCode\":\"12\"," +
                        "\"registrationAuthority\":\"ARS/ARS/ARS\",\"structure\":{\"siteSIRET\":\"125 137 196 15574\",\"siteSIREN\":\"125 137 196\"," +
                        "\"siteFINESS\":null,\"legalEstablishmentFINESS\":null,\"structureTechnicalId\":\"1\"," +
                        "\"legalCommercialName\":\"Structure One\",\"publicCommercialName\":\"Structure One\",\"recipientAdditionalInfo\":\"info +\"," +
                        "\"geoLocationAdditionalInfo\":\"geoloc info +\",\"streetNumber\":\"1\",\"streetNumberRepetitionIndex\":\"bis\"," +
                        "\"streetCategoryCode\":\"rue\",\"streetLabel\":\"Zorro\",\"distributionMention\":\"c/o Bernardo\",\"cedexOffice\":\"75117\"," +
                        "\"postalCode\":\"75017\",\"communeCode\":\"75\",\"countryCode\":\"FR\",\"phone\":\"0123456789\",\"phone2\":\"0623456789\"," +
                        "\"fax\":\"0198765432\",\"email\":\"structure@one.fr\",\"departmentCode\":\"99\",\"oldStructureId\":\"101\"," +
                        "\"registrationAuthority\":\"CIA\"}}]}]}"))
                .andExpect(status().is(201));
        assertThat(memoryAppender.contains("Ps 800000000001 successfully stored or updated", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("PsRef 800000000001 has been reactivated", Level.INFO)).isFalse();

        createdPs.andDo(document("PsOperationTest/create_new_Ps"));
    }

    @Test
    @DisplayName(value = "should reject post request if wrong content-type")
    public void createPsWrongContentTypeFailed() throws Exception {
        mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/xml").content("{\"idType\":\"8\",\"id\":\"00000000001\"," +
                        "\"nationalId\":\"800000000001\"}"))
                .andExpect(status().is(415));
        assertThat(memoryAppender.contains("Ps 800000000001 successfully stored or updated", Level.INFO)).isFalse();
        assertThat(memoryAppender.contains("PsRef 800000000001 has been reactivated", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should reject post request if content-type absent")
    public void createPsAbsentContentTypeFailed() throws Exception {
        mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .content("{\"idType\":\"8\",\"id\":\"00000000001\"," +
                        "\"nationalId\":\"800000000001\"}"))
                .andExpect(status().is(415));
        assertThat(memoryAppender.contains("Ps 800000000001 successfully stored or updated", Level.INFO)).isFalse();
        assertThat(memoryAppender.contains("PsRef 800000000001 has been reactivated", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should not create a Ps if already exists and still activated")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void createStillActivatedPsFailed() throws Exception {
        mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000001\",\n" +
                        "\"nationalId\": \"800000000001\"\n" +
                        "}"))
                .andExpect(status().is(409));
        assertThat(memoryAppender.contains("Ps 800000000001 already exists and is activated, will not be updated", Level.WARN)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000001 successfully stored or updated", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should reactivate Ps if already exists")
    @MongoDataSet(value = "/dataset/deactivated_ps.json", cleanBefore = true, cleanAfter = true)
    public void reactivateExistingPs() throws Exception {
        mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000002\",\n" +
                        "\"nationalId\": \"800000000002\"\n" +
                        "}"))
                .andExpect(status().is(201));
        assertThat(memoryAppender.contains("Ps 800000000002 successfully stored or updated", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("PsRef 800000000002 has been reactivated", Level.INFO)).isTrue();
    }

    @Test
    @DisplayName(value = "should not create Ps if malformed request body")
    public void createMalformedPsFailed() throws Exception {
        mockMvc.perform(post("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\"toto\":\"titi\"}"))
                .andExpect(status().is(400));
    }


    @Test
    @DisplayName(value = "should delete Ps by Id")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void deletePsById() throws Exception {
        ResultActions deletedPs = mockMvc.perform(delete("/api/v2/ps/800000000001"))
                .andExpect(status().is(204));

        assertThat(memoryAppender.contains("No Ps found with nationalId 800000000001, will not be deleted", Level.WARN)).isFalse();
        assertThat(memoryAppender.contains("Ps 800000000001 successfully deleted", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000011 successfully deleted", Level.INFO)).isTrue();

        PsRef psRef1 = psRefRepository.findPsRefByNationalIdRef("800000000001");
        PsRef psRef2 = psRefRepository.findPsRefByNationalIdRef("800000000011");

        assertThat(ApiUtils.isPsRefActivated(psRef1)).isFalse();
        assertThat(ApiUtils.isPsRefActivated(psRef2)).isFalse();

        deletedPs.andDo(document("PsOperationTest/delete_Ps_by_id"));
    }

    @Test
    @DisplayName(value = "should not delete Ps if not exists")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    @ExpectedMongoDataSet(value = "/dataset/ps_2_psref_entries.json")
    public void deletePsFailed() throws Exception {
        mockMvc.perform(delete("/api/v2/ps/800000000003")
                .header("Accept", "application/json"))
                .andExpect(status().is(410));

        assertThat(memoryAppender.contains("No Ps found with nationalId 800000000003, will not be deleted", Level.WARN)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000003 successfully deleted", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should update Ps")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void updatePs() throws Exception {
        ResultActions updatedPs = mockMvc.perform(put("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000001\",\n" +
                        "\"nationalId\": \"800000000001\"\n" +
                        "}"))
                .andExpect(status().is(200));

        assertThat(memoryAppender.contains("No Ps found with nationalId 800000000001, can not update it", Level.WARN)).isFalse();
        assertThat(memoryAppender.contains("Ps 800000000001 successfully updated", Level.INFO)).isTrue();

        updatedPs.andDo(document("PsOperationTest/update_Ps"));
    }

    @Test
    @DisplayName(value = "should not update Ps if not exists")
    public void updateAbsentPsFailed() throws Exception {
        mockMvc.perform(put("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000001\",\n" +
                        "\"nationalId\": \"800000000001\"\n" +
                        "}"))
                .andExpect(status().is(410));

        assertThat(memoryAppender.contains("No Ps found with nationalId 800000000001, can not update it", Level.WARN)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000001 successfully updated", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should not update Ps if deactivated")
    @MongoDataSet(value = "/dataset/deactivated_ps.json", cleanBefore = true, cleanAfter = true)
    public void updateDeactivatedPsFailed() throws Exception {
        mockMvc.perform(put("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000002\",\n" +
                        "\"nationalId\": \"800000000002\"\n" +
                        "}"))
                .andExpect(status().is(410));

        assertThat(memoryAppender.contains("No Ps found with nationalId 800000000002, can not update it", Level.WARN)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000002 successfully updated", Level.INFO)).isFalse();
    }

    @Test
    @DisplayName(value = "should not update Ps if malformed request body")
    @MongoDataSet(value = "/dataset/ps_2_psref_entries.json", cleanBefore = true, cleanAfter = true)
    public void updateMalformedPsFailed() throws Exception {
        // Id not present
        mockMvc.perform(put("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000001\",\n" +
                        "}"))
                .andExpect(status().is(400));

        // Id is blank
        mockMvc.perform(put("/api/v2/ps").header("Accept", "application/json")
                .contentType("application/json").content("{\n" +
                        "\"idType\": \"8\",\n" +
                        "\"id\": \"00000000001\",\n" +
                        "\"nationalId\": \"\"\n" +
                        "}"))
                .andExpect(status().is(400));
    }

    @Test
    @DisplayName(value = "should physically delete Ps")
    @MongoDataSet(value = "/dataset/3_ps_before_delete.json", cleanBefore = true, cleanAfter = true)
    @ExpectedMongoDataSet(value = "/dataset/1_ps_after_delete.json")
    public void physicalDeleteById() throws Exception {
        mockMvc.perform(delete("/api/v2/ps/force/800000000001")
                .header("Accept", "application/json"))
                .andExpect(status().is(204));

        assertThat(memoryAppender.contains("No Ps found with id 800000000001, could not delete it", Level.WARN)).isFalse();
        assertThat(memoryAppender.contains("Ps 800000000001 successfully deleted", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("PsRef 800000000001 pointing on Ps 800000000001 successfully removed", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("PsRef 800000000011 pointing on Ps 800000000001 successfully removed", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("Ps 800000000002 successfully deleted", Level.INFO)).isFalse();

        assertEquals(psRefRepository.count(), 2);
        assertEquals(psRepository.count(), 2);

        // physical delete of deactivated Ps
        mockMvc.perform(delete("/api/v2/ps/force/800000000002")
                .header("Accept", "application/json"))
                .andExpect(status().is(204));

        assertThat(memoryAppender.contains("Ps 800000000002 successfully deleted", Level.INFO)).isTrue();
        assertThat(memoryAppender.contains("PsRef 800000000002 pointing on Ps 800000000002 successfully removed", Level.INFO)).isTrue();

        assertEquals(psRefRepository.count(), 1);
        assertEquals(psRepository.count(), 1);
    }
}
