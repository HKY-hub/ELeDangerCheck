package org.example.eledangercheck.entity;

import java.util.ArrayList;
import java.util.List;

public class PpeDetectionResult {

    private int width;
    private int height;
    private List<PersonPpeInfo> persons = new ArrayList<>();
    private List<ViolationInfo> violations = new ArrayList<>();
    private String requestId;
    private boolean success = true;
    private String errorMsg;

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<PersonPpeInfo> getPersons() { return persons; }
    public void setPersons(List<PersonPpeInfo> persons) { this.persons = persons; }

    public List<ViolationInfo> getViolations() { return violations; }
    public void setViolations(List<ViolationInfo> violations) { this.violations = violations; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public static class PersonPpeInfo {
        private double x;
        private double y;
        private double width;
        private double height;
        private double detectConfidence;
        private boolean hasHelmet;
        private double helmetConfidence;
        private boolean hasVest;
        private double vestConfidence;
        private boolean hasSafetyBelt;
        private double safetyBeltConfidence;
        private boolean usingPhone;
        private double phoneConfidence;
        private boolean smoking;
        private double smokingConfidence;
        private List<PpeAttribute> attributes = new ArrayList<>();

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }

        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

        public double getDetectConfidence() { return detectConfidence; }
        public void setDetectConfidence(double detectConfidence) { this.detectConfidence = detectConfidence; }

        public boolean isHasHelmet() { return hasHelmet; }
        public void setHasHelmet(boolean hasHelmet) { this.hasHelmet = hasHelmet; }

        public double getHelmetConfidence() { return helmetConfidence; }
        public void setHelmetConfidence(double helmetConfidence) { this.helmetConfidence = helmetConfidence; }

        public boolean isHasVest() { return hasVest; }
        public void setHasVest(boolean hasVest) { this.hasVest = hasVest; }

        public double getVestConfidence() { return vestConfidence; }
        public void setVestConfidence(double vestConfidence) { this.vestConfidence = vestConfidence; }

        public boolean isHasSafetyBelt() { return hasSafetyBelt; }
        public void setHasSafetyBelt(boolean hasSafetyBelt) { this.hasSafetyBelt = hasSafetyBelt; }

        public double getSafetyBeltConfidence() { return safetyBeltConfidence; }
        public void setSafetyBeltConfidence(double safetyBeltConfidence) { this.safetyBeltConfidence = safetyBeltConfidence; }

        public boolean isUsingPhone() { return usingPhone; }
        public void setUsingPhone(boolean usingPhone) { this.usingPhone = usingPhone; }

        public double getPhoneConfidence() { return phoneConfidence; }
        public void setPhoneConfidence(double phoneConfidence) { this.phoneConfidence = phoneConfidence; }

        public boolean isSmoking() { return smoking; }
        public void setSmoking(boolean smoking) { this.smoking = smoking; }

        public double getSmokingConfidence() { return smokingConfidence; }
        public void setSmokingConfidence(double smokingConfidence) { this.smokingConfidence = smokingConfidence; }

        public List<PpeAttribute> getAttributes() { return attributes; }
        public void setAttributes(List<PpeAttribute> attributes) { this.attributes = attributes; }
    }

    public static class PpeAttribute {
        private String name;
        private String label;
        private double confidence;

        public PpeAttribute() {}

        public PpeAttribute(String name, String label, double confidence) {
            this.name = name;
            this.label = label;
            this.confidence = confidence;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    public static class ViolationInfo {
        private String type;
        private String description;
        private int personIndex;
        private double confidence;

        public ViolationInfo() {}

        public ViolationInfo(String type, String description, int personIndex, double confidence) {
            this.type = type;
            this.description = description;
            this.personIndex = personIndex;
            this.confidence = confidence;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getPersonIndex() { return personIndex; }
        public void setPersonIndex(int personIndex) { this.personIndex = personIndex; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
