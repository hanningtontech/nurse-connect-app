package com.example.nurse_connect.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Nursing Curriculum Structure
 * Defines the hierarchy: Career Level → Course → Unit
 */
public class NursingCurriculum {
    
    public static class CareerLevel {
        private String name;
        private String description;
        private List<Course> courses;
        
        public CareerLevel(String name, String description) {
            this.name = name;
            this.description = description;
            this.courses = new ArrayList<>();
        }
        
        public void addCourse(Course course) {
            courses.add(course);
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Course> getCourses() { return courses; }
    }
    
    public static class Course {
        private String name;
        private String description;
        private List<Unit> units;
        
        public Course(String name, String description) {
            this.name = name;
            this.description = description;
            this.units = new ArrayList<>();
        }
        
        public void addUnit(Unit unit) {
            units.add(unit);
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Unit> getUnits() { return units; }
    }
    
    public static class Unit {
        private String name;
        private String description;
        
        public Unit(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * Get the complete nursing curriculum structure
     * Based on your exact curriculum breakdown
     */
    public static List<CareerLevel> getCurriculum() {
        List<CareerLevel> careerLevels = new ArrayList<>();
        
        // 1. Certified Nursing Assistant (CNA)
        CareerLevel cna = new CareerLevel(
            "Certified Nursing Assistant (CNA)",
            "Program Type: Certificate"
        );
        
        Course cnaCourse = new Course(
            "Basic Nursing Skills / Nurse Aide Training",
            "Core CNA training program"
        );
        
        cnaCourse.addUnit(new Unit("Unit 1: Introduction to Healthcare", 
            "Role and Responsibilities of a CNA, Legal and Ethical Issues, Communication Skills"));
        cnaCourse.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", 
            "Infection Control, Emergency Response, Body Mechanics"));
        cnaCourse.addUnit(new Unit("Unit 3: Basic Patient Care", 
            "Activities of Daily Living (ADLs), Skin Care, Bedmaking"));
        cnaCourse.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", 
            "Temperature, Pulse, Respiration, Blood Pressure, Height and Weight"));
        cnaCourse.addUnit(new Unit("Unit 5: Nutrition and Hydration", 
            "Assisting with Meals, Monitoring Intake, Special Diets"));
        cnaCourse.addUnit(new Unit("Unit 6: Clinical Practicum", 
            "Supervised hands-on practice in clinical setting"));
        
        cna.addCourse(cnaCourse);
        careerLevels.add(cna);
        
        // 2. Licensed Practical/Vocational Nurse (LPN/LVN)
        CareerLevel lpn = new CareerLevel(
            "Licensed Practical/Vocational Nurse (LPN/LVN)",
            "Program Type: Diploma or Certificate"
        );
        
        Course lpnCourse1 = new Course("Anatomy and Physiology", 
            "Body Systems, Cellular Biology, Basic Chemistry");
        lpnCourse1.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Basic healthcare concepts"));
        lpnCourse1.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Safety protocols"));
        lpnCourse1.addUnit(new Unit("Unit 3: Basic Patient Care", "Patient care fundamentals"));
        lpnCourse1.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Patient monitoring"));
        lpnCourse1.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Patient nutrition"));
        lpnCourse1.addUnit(new Unit("Unit 6: Clinical Practicum", "Hands-on practice"));
        
        Course lpnCourse2 = new Course("Fundamentals of Nursing", 
            "Nursing Process, Legal/Ethical Principles, Documentation, Patient Safety, Basic Pharmacology");
        lpnCourse2.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Nursing fundamentals"));
        lpnCourse2.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Safety in nursing"));
        lpnCourse2.addUnit(new Unit("Unit 3: Basic Patient Care", "Patient care basics"));
        lpnCourse2.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Patient assessment"));
        lpnCourse2.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Patient nutrition"));
        lpnCourse2.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course lpnCourse3 = new Course("Pharmacology for LPNs", 
            "Medication Classifications, Routes of Administration, Dosage Calculations, Side Effects");
        lpnCourse3.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Pharmacology basics"));
        lpnCourse3.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Medication safety"));
        lpnCourse3.addUnit(new Unit("Unit 3: Basic Patient Care", "Medication administration"));
        lpnCourse3.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Patient monitoring"));
        lpnCourse3.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Medication interactions"));
        lpnCourse3.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical practice"));
        
        Course lpnCourse4 = new Course("Medical-Surgical Nursing", 
            "Care for adults with common conditions, Pre/Post-Operative Care, Wound Care");
        lpnCourse4.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Medical-surgical basics"));
        lpnCourse4.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Surgical safety"));
        lpnCourse4.addUnit(new Unit("Unit 3: Basic Patient Care", "Surgical patient care"));
        lpnCourse4.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Post-op monitoring"));
        lpnCourse4.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Post-op nutrition"));
        lpnCourse4.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course lpnCourse5 = new Course("Maternal-Child Nursing", 
            "Basic care for pregnant women, newborns, and children");
        lpnCourse5.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Maternal-child basics"));
        lpnCourse5.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Maternal safety"));
        lpnCourse5.addUnit(new Unit("Unit 3: Basic Patient Care", "Maternal care"));
        lpnCourse5.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Maternal monitoring"));
        lpnCourse5.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Maternal nutrition"));
        lpnCourse5.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course lpnCourse6 = new Course("Gerontological Nursing", 
            "Care for the elderly, common age-related conditions, End-of-Life Care");
        lpnCourse6.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Geriatric basics"));
        lpnCourse6.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Geriatric safety"));
        lpnCourse6.addUnit(new Unit("Unit 3: Basic Patient Care", "Geriatric care"));
        lpnCourse6.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Geriatric monitoring"));
        lpnCourse6.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Geriatric nutrition"));
        lpnCourse6.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        lpn.addCourse(lpnCourse1);
        lpn.addCourse(lpnCourse2);
        lpn.addCourse(lpnCourse3);
        lpn.addCourse(lpnCourse4);
        lpn.addCourse(lpnCourse5);
        lpn.addCourse(lpnCourse6);
        careerLevels.add(lpn);
        
        // 3. Registered Nurse (RN) - BSN Level
        CareerLevel rn = new CareerLevel(
            "Registered Nurse (RN) - BSN Level",
            "Program Type: Bachelor of Science in Nursing (BSN)"
        );
        
        Course rnCourse1 = new Course("Health Assessment", 
            "Comprehensive Head-to-Toe Physical Assessment, Health History Taking, Differentiating Normal/Abnormal Findings");
        rnCourse1.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Health assessment basics"));
        rnCourse1.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Assessment safety"));
        rnCourse1.addUnit(new Unit("Unit 3: Basic Patient Care", "Patient assessment"));
        rnCourse1.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Assessment monitoring"));
        rnCourse1.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Nutrition assessment"));
        rnCourse1.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical assessment"));
        
        Course rnCourse2 = new Course("Pathophysiology", 
            "The study of how diseases alter normal body functions");
        rnCourse2.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Pathophysiology basics"));
        rnCourse2.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Disease safety"));
        rnCourse2.addUnit(new Unit("Unit 3: Basic Patient Care", "Disease care"));
        rnCourse2.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Disease monitoring"));
        rnCourse2.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Disease nutrition"));
        rnCourse2.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical practice"));
        
        Course rnCourse3 = new Course("Advanced Pharmacology", 
            "Drug Mechanisms of Action, Pharmacokinetics, Complex Medication Management");
        rnCourse3.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Advanced pharmacology basics"));
        rnCourse3.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Advanced medication safety"));
        rnCourse3.addUnit(new Unit("Unit 3: Basic Patient Care", "Advanced medication care"));
        rnCourse3.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Advanced monitoring"));
        rnCourse3.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Advanced interactions"));
        rnCourse3.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical practice"));
        
        Course rnCourse4 = new Course("Medical-Surgical Nursing I & II", 
            "Management of complex adult health problems across all body systems, critical thinking in acute care");
        rnCourse4.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Medical-surgical basics"));
        rnCourse4.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Surgical safety"));
        rnCourse4.addUnit(new Unit("Unit 3: Basic Patient Care", "Surgical care"));
        rnCourse4.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Surgical monitoring"));
        rnCourse4.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Surgical nutrition"));
        rnCourse4.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course rnCourse5 = new Course("Mental Health Nursing", 
            "Therapeutic Communication, Psychiatric Disorders, Psychotropic Medications, Crisis Intervention");
        rnCourse5.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Mental health basics"));
        rnCourse5.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Mental health safety"));
        rnCourse5.addUnit(new Unit("Unit 3: Basic Patient Care", "Mental health care"));
        rnCourse5.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Mental health monitoring"));
        rnCourse5.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Mental health nutrition"));
        rnCourse5.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course rnCourse6 = new Course("Obstetrics (OB) / Women's Health Nursing", 
            "Prenatal Care, Labor and Delivery, Postpartum Care, Newborn Assessment");
        rnCourse6.addUnit(new Unit("Unit 1: Introduction to Healthcare", "OB basics"));
        rnCourse6.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "OB safety"));
        rnCourse6.addUnit(new Unit("Unit 3: Basic Patient Care", "OB care"));
        rnCourse6.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "OB monitoring"));
        rnCourse6.addUnit(new Unit("Unit 5: Nutrition and Hydration", "OB nutrition"));
        rnCourse6.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course rnCourse7 = new Course("Pediatric Nursing", 
            "Growth and Development Milestones, Childhood Diseases, Family-Centered Care");
        rnCourse7.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Pediatric basics"));
        rnCourse7.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Pediatric safety"));
        rnCourse7.addUnit(new Unit("Unit 3: Basic Patient Care", "Pediatric care"));
        rnCourse7.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Pediatric monitoring"));
        rnCourse7.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Pediatric nutrition"));
        rnCourse7.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course rnCourse8 = new Course("Community & Public Health Nursing", 
            "Health Promotion in Populations, Epidemiology, Healthcare Policy");
        rnCourse8.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Community health basics"));
        rnCourse8.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Community safety"));
        rnCourse8.addUnit(new Unit("Unit 3: Basic Patient Care", "Community care"));
        rnCourse8.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Community monitoring"));
        rnCourse8.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Community nutrition"));
        rnCourse8.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        Course rnCourse9 = new Course("Nursing Leadership & Management", 
            "Leadership Principles, Healthcare Finance, Quality Improvement, Delegation");
        rnCourse9.addUnit(new Unit("Unit 1: Introduction to Healthcare", "Leadership basics"));
        rnCourse9.addUnit(new Unit("Unit 2: Safety and Emergency Procedures", "Leadership safety"));
        rnCourse9.addUnit(new Unit("Unit 3: Basic Patient Care", "Leadership care"));
        rnCourse9.addUnit(new Unit("Unit 4: Vital Signs and Monitoring", "Leadership monitoring"));
        rnCourse9.addUnit(new Unit("Unit 5: Nutrition and Hydration", "Leadership nutrition"));
        rnCourse9.addUnit(new Unit("Unit 6: Clinical Practicum", "Clinical experience"));
        
        rn.addCourse(rnCourse1);
        rn.addCourse(rnCourse2);
        rn.addCourse(rnCourse3);
        rn.addCourse(rnCourse4);
        rn.addCourse(rnCourse5);
        rn.addCourse(rnCourse6);
        rn.addCourse(rnCourse7);
        rn.addCourse(rnCourse8);
        rn.addCourse(rnCourse9);
        careerLevels.add(rn);
        
        return careerLevels;
    }
}
