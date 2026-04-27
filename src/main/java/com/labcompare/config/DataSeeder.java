package com.labcompare.config;

import com.labcompare.model.*;
import com.labcompare.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(LabRepository labRepo, TestRepository testRepo,
                               LabTestPriceRepository priceRepo, UserRepository userRepo,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            if (!userRepo.existsByUsername("superadmin")) {
                userRepo.save(new User("superadmin", passwordEncoder.encode("super123"), User.Role.SUPERADMIN));
                System.out.println("[LabCompare] SuperAdmin created: superadmin / super123");
            }
            if (!userRepo.existsByUsername("admin")) {
                userRepo.save(new User("admin", passwordEncoder.encode("admin123"), User.Role.ADMIN));
                System.out.println("[LabCompare] Generic admin created: admin / admin123");
            }
            if (!userRepo.existsByUsername("user")) {
                userRepo.save(new User("user", passwordEncoder.encode("user123"), User.Role.USER));
                System.out.println("[LabCompare] Default user created: user / user123");
            }

            if (labRepo.count() > 0) {
                seedMissingTests(testRepo, priceRepo, labRepo);
                return;
            }

            System.out.println("[LabCompare] Seeding initial data...");

            Lab lal      = saveLab(labRepo, "Dr Lal PathLabs",      "Mumbai", "Andheri West",    "1800-102-5555", 4.6, "NABL",      "drlalpathlab@upi");
            Lab srl      = saveLab(labRepo, "SRL Diagnostics",      "Mumbai", "Bandra East",     "1800-222-333",  4.4, "NABL",      "srldiagnostics@upi");
            Lab thyro    = saveLab(labRepo, "Thyrocare",            "Mumbai", "Navi Mumbai",     "1800-419-6272", 4.3, "NABL",      "thyrocare@upi");
            Lab metro    = saveLab(labRepo, "Metropolis",           "Mumbai", "Dadar",           "022-3399-3939", 4.5, "NABL, CAP", "metropolislab@upi");
            Lab suburban = saveLab(labRepo, "Suburban Diagnostics", "Pune",   "Kothrud",         "020-1234-5678", 4.2, "NABL",      "suburbandiag@upi");
            Lab apollo   = saveLab(labRepo, "Apollo Diagnostics",   "Delhi",  "Connaught Place", "011-4040-4040", 4.5, "NABL, ISO", "apollodiag@upi");

            createLabAdmin(userRepo, passwordEncoder, "admin_lal",      "lal123",      lal.getId());
            createLabAdmin(userRepo, passwordEncoder, "admin_srl",      "srl123",      srl.getId());
            createLabAdmin(userRepo, passwordEncoder, "admin_thyro",    "thyro123",    thyro.getId());
            createLabAdmin(userRepo, passwordEncoder, "admin_metro",    "metro123",    metro.getId());
            createLabAdmin(userRepo, passwordEncoder, "admin_suburban", "suburban123", suburban.getId());
            createLabAdmin(userRepo, passwordEncoder, "admin_apollo",   "apollo123",   apollo.getId());

            Test cbc     = saveTest(testRepo, "Complete Blood Count (CBC)",  "Blood",     "Evaluates overall blood health");
            Test thyroid = saveTest(testRepo, "Thyroid Profile (T3/T4/TSH)", "Thyroid",   "Checks thyroid hormone levels");
            Test lft     = saveTest(testRepo, "Liver Function Test (LFT)",   "Liver",     "Assesses liver health and function");
            Test lipid   = saveTest(testRepo, "Lipid Profile",               "Cardiac",   "Measures cholesterol and triglycerides");
            Test hba1c   = saveTest(testRepo, "HbA1c (Diabetes)",            "Diabetes",  "3-month average blood sugar indicator");
            Test vitD    = saveTest(testRepo, "Vitamin D",                   "Vitamins",  "Checks vitamin D levels in blood");
            Test kft     = saveTest(testRepo, "Kidney Function Test (KFT)",  "Kidney",    "Evaluates kidney health");
            Test urine   = saveTest(testRepo, "Urine Routine",               "Urine",     "Basic urine analysis");
            Test dengue  = saveTest(testRepo, "Dengue NS1 Antigen",          "Infection", "Early detection of dengue fever");
            Test covid   = saveTest(testRepo, "COVID-19 RT-PCR",             "Infection", "Detects active COVID-19 infection");
            Test mri     = saveTest(testRepo, "Brain MRI",                   "Radiology", "MRI of the brain");
            Test bm      = saveTest(testRepo, "Bone Marrow Test",            "Pathology", "Bone marrow biopsy and examination");
            Test esr     = saveTest(testRepo, "ESR",                         "Blood",     "Erythrocyte Sedimentation Rate");
            Test vitB12  = saveTest(testRepo, "Vitamin B12",                 "Vitamins",  "Vitamin B12 level in blood");
            Test ecg     = saveTest(testRepo, "ECG",                         "Cardiac",   "Electrocardiogram");
            Test xray    = saveTest(testRepo, "X-Ray",                       "Radiology", "Radiographic imaging");
            Test ct      = saveTest(testRepo, "CT Scan",                     "Radiology", "Computed tomography scan");
            Test us      = saveTest(testRepo, "Ultrasound",                  "Radiology", "Ultrasound imaging");

            priceRepo.saveAll(Arrays.asList(
                p(lal,cbc,350,0,"Same Day"),    p(srl,cbc,320,5,"Same Day"),
                p(thyro,cbc,299,10,"Next Day"), p(metro,cbc,380,0,"Same Day"),
                p(suburban,cbc,310,0,"Same Day"),p(apollo,cbc,360,0,"Same Day"),

                p(lal,thyroid,650,0,"Same Day"),    p(srl,thyroid,580,0,"Same Day"),
                p(thyro,thyroid,499,10,"Next Day"),  p(metro,thyroid,700,5,"Same Day"),
                p(suburban,thyroid,530,0,"Same Day"),p(apollo,thyroid,620,0,"Same Day"),

                p(lal,lft,900,0,"Same Day"),    p(srl,lft,850,0,"Same Day"),
                p(thyro,lft,749,0,"Next Day"),  p(metro,lft,950,10,"Same Day"),
                p(suburban,lft,780,0,"Same Day"),p(apollo,lft,880,5,"Same Day"),

                p(lal,lipid,700,0,"Same Day"),  p(srl,lipid,650,5,"Same Day"),
                p(thyro,lipid,599,10,"Next Day"),p(metro,lipid,750,0,"Same Day"),
                p(suburban,lipid,620,0,"Same Day"),

                p(lal,hba1c,500,0,"Same Day"),  p(srl,hba1c,450,0,"Same Day"),
                p(thyro,hba1c,399,10,"Next Day"),p(metro,hba1c,550,0,"Same Day"),
                p(apollo,hba1c,480,0,"Same Day"),

                p(lal,vitD,1200,0,"Same Day"),  p(srl,vitD,999,0,"Same Day"),
                p(thyro,vitD,899,10,"Next Day"),p(metro,vitD,1100,5,"Same Day"),
                p(apollo,vitD,1050,0,"Same Day"),

                p(lal,kft,800,0,"Same Day"),    p(srl,kft,750,5,"Same Day"),
                p(thyro,kft,649,0,"Next Day"),  p(metro,kft,850,0,"Same Day"),
                p(suburban,kft,700,0,"Same Day"),

                p(lal,urine,250,0,"Same Day"),  p(srl,urine,230,0,"Same Day"),
                p(thyro,urine,199,0,"Next Day"),p(metro,urine,270,0,"Same Day"),
                p(suburban,urine,220,0,"Same Day"),

                p(lal,dengue,900,0,"Same Day"), p(srl,dengue,850,0,"4 Hours"),
                p(metro,dengue,950,5,"Same Day"),p(apollo,dengue,880,0,"4 Hours"),

                p(lal,covid,500,0,"24 Hours"),  p(srl,covid,450,10,"24 Hours"),
                p(metro,covid,550,0,"24 Hours"),p(apollo,covid,480,0,"12 Hours"),

                p(lal,mri,3500,0,"Same Day"),   p(srl,mri,3200,5,"Same Day"),
                p(metro,mri,3800,0,"Same Day"),  p(apollo,mri,3600,0,"Same Day"),

                p(lal,bm,2500,0,"2 Days"),  p(srl,bm,2200,0,"2 Days"),
                p(metro,bm,2800,0,"2 Days"), p(apollo,bm,2600,0,"2 Days"),

                p(lal,esr,150,0,"Same Day"),    p(srl,esr,130,0,"Same Day"),
                p(metro,esr,180,0,"Same Day"),   p(apollo,esr,160,0,"Same Day"),
                p(suburban,esr,120,0,"Same Day"),

                p(lal,vitB12,800,0,"Same Day"), p(srl,vitB12,750,0,"Same Day"),
                p(metro,vitB12,900,0,"Same Day"),p(apollo,vitB12,850,0,"Same Day"),

                p(lal,ecg,300,0,"Same Day"),    p(srl,ecg,280,5,"Same Day"),
                p(metro,ecg,350,0,"Same Day"),   p(apollo,ecg,320,0,"Same Day"),
                p(suburban,ecg,260,0,"Same Day"),

                p(lal,xray,400,0,"Same Day"),   p(srl,xray,350,0,"Same Day"),
                p(metro,xray,450,0,"Same Day"),  p(apollo,xray,400,0,"Same Day"),
                p(suburban,xray,320,0,"Same Day"),

                p(lal,ct,4500,0,"Same Day"),    p(srl,ct,4200,5,"Same Day"),
                p(metro,ct,4800,10,"Same Day"),  p(apollo,ct,4600,0,"Same Day"),

                p(lal,us,800,0,"Same Day"),  p(srl,us,750,5,"Same Day"),
                p(metro,us,900,0,"Same Day"), p(apollo,us,850,0,"Same Day"),
                p(suburban,us,700,0,"Same Day")
            ));

            System.out.println("[LabCompare] Seeded " + labRepo.count() + " labs, " +
                               testRepo.count() + " tests, " + priceRepo.count() + " prices.");
        };
    }

    private void seedMissingTests(TestRepository testRepo, LabTestPriceRepository priceRepo, LabRepository labRepo) {
        java.util.List<Lab> labs = labRepo.findAll();
        if (labs.isEmpty()) { return; }

        Lab lal      = labs.stream().filter(l -> l.getName().contains("Lal")).findFirst().orElse(labs.get(0));
        Lab srl      = labs.stream().filter(l -> l.getName().contains("SRL")).findFirst().orElse(labs.get(0));
        Lab metro    = labs.stream().filter(l -> l.getName().contains("Metropolis")).findFirst().orElse(labs.get(0));
        Lab apollo   = labs.stream().filter(l -> l.getName().contains("Apollo")).findFirst().orElse(labs.get(0));
        Lab suburban = labs.stream().filter(l -> l.getName().contains("Suburban")).findFirst().orElse(labs.get(0));

        if (testRepo.findByNameIgnoreCase("Brain MRI").isEmpty()) {
            Test t = saveTest(testRepo, "Brain MRI", "Radiology", "MRI of the brain");
            priceRepo.saveAll(Arrays.asList(p(lal,t,3500,0,"Same Day"),p(srl,t,3200,5,"Same Day"),p(metro,t,3800,0,"Same Day"),p(apollo,t,3600,0,"Same Day")));
            System.out.println("[LabCompare] Added: Brain MRI");
        }
        if (testRepo.findByNameIgnoreCase("Bone Marrow Test").isEmpty()) {
            Test t = saveTest(testRepo, "Bone Marrow Test", "Pathology", "Bone marrow biopsy");
            priceRepo.saveAll(Arrays.asList(p(lal,t,2500,0,"2 Days"),p(srl,t,2200,0,"2 Days"),p(metro,t,2800,0,"2 Days"),p(apollo,t,2600,0,"2 Days")));
            System.out.println("[LabCompare] Added: Bone Marrow Test");
        }
        if (testRepo.findByNameIgnoreCase("ESR").isEmpty()) {
            Test t = saveTest(testRepo, "ESR", "Blood", "Erythrocyte Sedimentation Rate");
            priceRepo.saveAll(Arrays.asList(p(lal,t,150,0,"Same Day"),p(srl,t,130,0,"Same Day"),p(metro,t,180,0,"Same Day"),p(apollo,t,160,0,"Same Day"),p(suburban,t,120,0,"Same Day")));
            System.out.println("[LabCompare] Added: ESR");
        }
        if (testRepo.findByNameIgnoreCase("Vitamin B12").isEmpty()) {
            Test t = saveTest(testRepo, "Vitamin B12", "Vitamins", "Vitamin B12 level");
            priceRepo.saveAll(Arrays.asList(p(lal,t,800,0,"Same Day"),p(srl,t,750,0,"Same Day"),p(metro,t,900,0,"Same Day"),p(apollo,t,850,0,"Same Day")));
            System.out.println("[LabCompare] Added: Vitamin B12");
        }
        if (testRepo.findByNameIgnoreCase("ECG").isEmpty()) {
            Test t = saveTest(testRepo, "ECG", "Cardiac", "Electrocardiogram");
            priceRepo.saveAll(Arrays.asList(p(lal,t,300,0,"Same Day"),p(srl,t,280,5,"Same Day"),p(metro,t,350,0,"Same Day"),p(apollo,t,320,0,"Same Day"),p(suburban,t,260,0,"Same Day")));
            System.out.println("[LabCompare] Added: ECG");
        }
        if (testRepo.findByNameIgnoreCase("X-Ray").isEmpty()) {
            Test t = saveTest(testRepo, "X-Ray", "Radiology", "Radiographic imaging");
            priceRepo.saveAll(Arrays.asList(p(lal,t,400,0,"Same Day"),p(srl,t,350,0,"Same Day"),p(metro,t,450,0,"Same Day"),p(apollo,t,400,0,"Same Day"),p(suburban,t,320,0,"Same Day")));
            System.out.println("[LabCompare] Added: X-Ray");
        }
        if (testRepo.findByNameIgnoreCase("CT Scan").isEmpty()) {
            Test t = saveTest(testRepo, "CT Scan", "Radiology", "Computed tomography scan");
            priceRepo.saveAll(Arrays.asList(p(lal,t,4500,0,"Same Day"),p(srl,t,4200,5,"Same Day"),p(metro,t,4800,10,"Same Day"),p(apollo,t,4600,0,"Same Day")));
            System.out.println("[LabCompare] Added: CT Scan");
        }
        if (testRepo.findByNameIgnoreCase("Ultrasound").isEmpty()) {
            Test t = saveTest(testRepo, "Ultrasound", "Radiology", "Ultrasound imaging");
            priceRepo.saveAll(Arrays.asList(p(lal,t,800,0,"Same Day"),p(srl,t,750,5,"Same Day"),p(metro,t,900,0,"Same Day"),p(apollo,t,850,0,"Same Day"),p(suburban,t,700,0,"Same Day")));
            System.out.println("[LabCompare] Added: Ultrasound");
        }
    }

    private void createLabAdmin(UserRepository repo, PasswordEncoder enc, String username, String password, Long labId) {
        if (!repo.existsByUsername(username)) {
            repo.save(new User(username, enc.encode(password), User.Role.ADMIN, labId));
            System.out.println("[LabCompare] Lab admin: " + username + " / " + password + " labId=" + labId);
        }
    }

    private Lab saveLab(LabRepository repo, String name, String city, String address, String phone, double rating, String accred, String upiId) {
        Lab lab = new Lab();
        lab.setName(name); lab.setCity(city); lab.setAddress(address); lab.setPhone(phone);
        lab.setRating(rating); lab.setAccreditation(accred); lab.setHomeCollection(true); lab.setUpiId(upiId);
        return repo.save(lab);
    }

    private Test saveTest(TestRepository repo, String name, String category, String description) {
        Test test = new Test();
        test.setName(name); test.setCategory(category); test.setDescription(description);
        return repo.save(test);
    }

    private LabTestPrice p(Lab lab, Test test, double price, double discount, String duration) {
        LabTestPrice ltp = new LabTestPrice();
        ltp.setLab(lab); ltp.setTest(test); ltp.setPrice(price);
        ltp.setDiscountPercent(discount); ltp.setReportDuration(duration);
        return ltp;
    }
}