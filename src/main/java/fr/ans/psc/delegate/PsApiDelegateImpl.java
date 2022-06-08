package fr.ans.psc.delegate;

import fr.ans.psc.api.PsApiDelegate;
import fr.ans.psc.model.Ps;
import fr.ans.psc.model.PsRef;
import fr.ans.psc.repository.PsRefRepository;
import fr.ans.psc.repository.PsRepository;
import fr.ans.psc.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PsApiDelegateImpl implements PsApiDelegate {

    private final PsRepository psRepository;
    private final PsRefRepository psRefRepository;
    private final MongoTemplate mongoTemplate;

    public PsApiDelegateImpl(PsRepository psRepository, PsRefRepository psRefRepository, MongoTemplate mongoTemplate) {
        this.psRepository = psRepository;
        this.psRefRepository = psRefRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ResponseEntity<Ps> getPsById(String encodedPsId) {
        String psId = URLDecoder.decode(encodedPsId, StandardCharsets.UTF_8);
        PsRef psRef = psRefRepository.findPsRefByNationalIdRef(psId);

        // check if PsRef exists and is activated
        if (!ApiUtils.isPsRefActivated(psRef)) {
            String operationLog = psRef == null ? "No Ps found with nationalIdRef {}" : "Ps {} is deactivated";
            log.warn(operationLog, psId);
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        String nationalId = psRef.getNationalId();
        Ps ps = psRepository.findByNationalId(nationalId);
        List<PsRef> allPsRefs = psRefRepository.findAllByNationalId(nationalId);
        ps.extractOtherIds(allPsRefs);
        log.info("Ps {} has been found", nationalId);
        return new ResponseEntity<>(ps, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> createNewPs(Ps ps) {
        long timestamp = ApiUtils.getInstantTimestamp();
        PsRef storedPsRef = psRefRepository.findPsRefByNationalIdRef(ps.getNationalId());

        // DON'T UPDATE IF ALREADY ACTIVATED
        if (ApiUtils.isPsRefActivated(storedPsRef)) {
            log.warn("Ps {} already exists and is activated, will not be updated", ps.getNationalId());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // PSREF EXIST, UPDATE AND REACTIVATION
        if (storedPsRef != null) {
            Ps storedPs = psRepository.findByNationalId(storedPsRef.getNationalId());
            if (storedPs != null) {
                // set mongo _id to avoid error if it's an update
                // Then update Ps data
                log.info("Ps {} already exists, will be updated", ps.getNationalId());
                ps.set_id(storedPs.get_id());
                mongoTemplate.save(ps);
                log.info("Ps {} successfully stored or updated", ps.getNationalId());

                // REACTIVATE ALL PSREF THAT POINTED TOWARDS UPDATED PS
                // It's programmatically possible that the updated Ps has a modified nationalId (and still the same mongo _id)
                // So we reset every PsRef pointer with the nationalId of the updated Ps no matter that it has actually changed or not
                List<PsRef> psRefList = psRefRepository.findAllByNationalId(storedPs.getNationalId());
                log.info("psRefList size {}", psRefList.size());
                psRefList.stream().filter(psRef -> !ApiUtils.isPsRefActivated(psRef)).forEach(psRef -> {
                    psRef.setActivated(timestamp);
                    psRef.setNationalId(ps.getNationalId());
                    mongoTemplate.save(psRef);
                    log.info("PsRef {} has been reactivated", psRef.getNationalIdRef());
                });

            }
        }
        // PREF DOES NOT EXIST, PHYSICAL CREATION
        else {
            log.info("PS {} doesn't exist already, will be created", ps.getNationalId());
            mongoTemplate.save(ps);
            log.info("Ps {} successfully stored or updated", ps.getNationalId());

            storedPsRef = new PsRef(ps.getNationalId(), ps.getNationalId(), timestamp);
            mongoTemplate.save(storedPsRef);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> updatePs(Ps ps) {
        // check if PsRef is activated before trying to update it
        PsRef storedPsRef = psRefRepository.findPsRefByNationalIdRef(ps.getNationalId());
        if (!ApiUtils.isPsRefActivated(storedPsRef)) {
            log.warn("No Ps found with nationalId {}, can not update it", ps.getNationalId());
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        // set technical id then update
        Ps storedPs = psRepository.findByNationalId(ps.getNationalId());
        if (storedPs != null) {
            ps.set_id(storedPs.get_id());
        }
        mongoTemplate.save(ps);
        log.info("Ps {} successfully updated", ps.getNationalId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deletePsById(String encodedPsId) {
        String psId = URLDecoder.decode(encodedPsId, StandardCharsets.UTF_8);
        PsRef storedPsRef = psRefRepository.findPsRefByNationalIdRef(psId);
        if (storedPsRef == null) {
            log.warn("No Ps found with nationalId {}, will not be deleted", psId);
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        // get all PsRefs that point to this ps
        List<PsRef> psRefList = psRefRepository.findAllByNationalId(storedPsRef.getNationalId());

        // deactivate each PsRef pointing to this ps
        long timestamp = ApiUtils.getInstantTimestamp();

        psRefList.forEach(psRef -> {
            psRef.setDeactivated(timestamp);
            mongoTemplate.save(psRef);
            log.info("Ps {} successfully deleted", psRef.getNationalIdRef());
        });

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> forceDeletePsById(String encodedPsId) {
        String psId = URLDecoder.decode(encodedPsId, StandardCharsets.UTF_8);
        Ps ps = psRepository.findByNationalId(psId);

        if (ps == null) {
            log.warn("No Ps found with id {}, could not delete it", psId);
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        mongoTemplate.remove(ps);
        log.info("Ps {} successfully deleted", psId);

        List<PsRef> psRefList = psRefRepository.findAllByNationalId(psId);
        psRefList.forEach(psRef -> {
            mongoTemplate.remove(psRef);
            log.info("PsRef {} pointing on Ps {} successfully removed", psRef.getNationalIdRef(), psId);
        });
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
