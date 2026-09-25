package org.example.eledangercheck.util;

import com.tencentcloudapi.tiia.v20190529.models.AttributesForBody;
import com.tencentcloudapi.tiia.v20190529.models.BodyAttributes;
import com.tencentcloudapi.tiia.v20190529.models.DetectSecurityResponse;
import com.tencentcloudapi.tiia.v20190529.models.ImageRect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.eledangercheck.entity.PpeDetectionResult;

import java.util.ArrayList;
import java.util.List;

public class PpeResultParseUtil {

    private static final Logger logger = LoggerFactory.getLogger(PpeResultParseUtil.class);
    private static final double CONF_THRESHOLD = 0.3;

    public static PpeDetectionResult parse(DetectSecurityResponse response, int imgWidth, int imgHeight) {
        PpeDetectionResult result = new PpeDetectionResult();
        result.setWidth(imgWidth);
        result.setHeight(imgHeight);

        if (response == null) {
            result.setSuccess(false);
            result.setErrorMsg("Response is null");
            return result;
        }

        result.setRequestId(response.getRequestId());

        AttributesForBody[] bodies = response.getBodies();
        if (bodies == null || bodies.length == 0) {
            logger.debug("No bodies detected");
            return result;
        }

        List<PpeDetectionResult.PersonPpeInfo> persons = new ArrayList<>();
        List<PpeDetectionResult.ViolationInfo> violations = new ArrayList<>();

        for (int i = 0; i < bodies.length; i++) {
            AttributesForBody body = bodies[i];
            PpeDetectionResult.PersonPpeInfo person = new PpeDetectionResult.PersonPpeInfo();

            ImageRect rect = body.getRect();
            if (rect != null) {
                person.setX(rect.getX() != null ? rect.getX().doubleValue() : 0);
                person.setY(rect.getY() != null ? rect.getY().doubleValue() : 0);
                person.setWidth(rect.getWidth() != null ? rect.getWidth().doubleValue() : 0);
                person.setHeight(rect.getHeight() != null ? rect.getHeight().doubleValue() : 0);
            }

            if (body.getDetectConfidence() != null) {
                person.setDetectConfidence(body.getDetectConfidence());
            }

            List<PpeDetectionResult.PpeAttribute> attributes = new ArrayList<>();

            BodyAttributes[] attrs = body.getAttributes();
            if (attrs != null) {
                for (BodyAttributes attr : attrs) {
                    String name = attr.getName() != null ? attr.getName() : "";
                    String label = attr.getLabel() != null ? attr.getLabel() : "";
                    double conf = attr.getConfidence() != null ? attr.getConfidence() : 0;

                    attributes.add(new PpeDetectionResult.PpeAttribute(name, label, conf));

                    switch (name) {
                        case "安全帽识别" -> {
                            if (conf >= CONF_THRESHOLD) {
                                person.setHasHelmet("有安全帽".equals(label));
                                person.setHelmetConfidence(conf);
                                if ("无安全帽".equals(label)) {
                                    violations.add(new PpeDetectionResult.ViolationInfo(
                                        "no_helmet", "未佩戴安全帽", i, conf));
                                }
                            }
                        }
                        case "反光衣识别" -> {
                            if (conf >= CONF_THRESHOLD) {
                                person.setHasVest("有反光衣".equals(label));
                                person.setVestConfidence(conf);
                                if ("无反光衣".equals(label)) {
                                    violations.add(new PpeDetectionResult.ViolationInfo(
                                        "no_vest", "未穿反光衣", i, conf));
                                }
                            }
                        }
                        case "工地安全带识别" -> {
                            if (conf >= CONF_THRESHOLD) {
                                person.setHasSafetyBelt("有工地安全带".equals(label));
                                person.setSafetyBeltConfidence(conf);
                                if ("无工地安全带".equals(label)) {
                                    violations.add(new PpeDetectionResult.ViolationInfo(
                                        "no_safety_belt", "未系安全带", i, conf));
                                }
                            }
                        }
                        case "施工手套识别" -> {
                        }
                        case "workerPhone" -> {
                            if (conf >= CONF_THRESHOLD) {
                                boolean isPhone = "看手机".equals(label) || "玩手机".equals(label);
                                person.setUsingPhone(isPhone);
                                person.setPhoneConfidence(conf);
                                if (isPhone) {
                                    violations.add(new PpeDetectionResult.ViolationInfo(
                                        "using_phone", "作业时玩手机", i, conf));
                                }
                            }
                        }
                        case "workerSmoke" -> {
                            if (conf >= CONF_THRESHOLD) {
                                boolean isSmoking = "吸烟".equals(label) || "抽烟".equals(label);
                                person.setSmoking(isSmoking);
                                person.setSmokingConfidence(conf);
                                if (isSmoking) {
                                    violations.add(new PpeDetectionResult.ViolationInfo(
                                        "smoking", "现场抽烟", i, conf));
                                }
                            }
                        }
                        default -> {
                        }
                    }
                }
            }

            person.setAttributes(attributes);
            persons.add(person);
        }

        result.setPersons(persons);
        result.setViolations(violations);

        logger.info("PPE parse result: {} persons, {} violations", persons.size(), violations.size());
        return result;
    }
}
