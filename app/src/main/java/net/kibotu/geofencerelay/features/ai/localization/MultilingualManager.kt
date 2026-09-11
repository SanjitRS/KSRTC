package net.kibotu.geofencerelay.features.ai.localization

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
)

object MultilingualManager {

    val supportedLanguages = listOf(
        LanguageItem("en", "English", "English", "🌐"),
        LanguageItem("hi", "Hindi", "हिंदी", "🇮🇳"),
        LanguageItem("as", "Assamese", "অসমীয়া", "🌿"),
        LanguageItem("lus", "Mizo", "Mizo ṭawng", "🏔️"),
        LanguageItem("kha", "Khasi", "Ka Ktien Khasi", "🌲"),
        LanguageItem("mni", "Manipuri", "মৈতৈলোন্", "🌺"),
        LanguageItem("nag", "Nagamese", "Nagamese", "⛰️")
    )

    // Full UI String Translations Dictionary
    private val stringRepository = mapOf(

        // Authentication & Login Screen
        "auth_tagline" to mapOf(
            "en" to "Dementia Care & Regional Heritage Companion",
            "hi" to "डिमेंशिया देखभाल और क्षेत्रीय विरासत साथी",
            "as" to "ডিমেনচিয়া যত্ন আৰু আঞ্চলিক ঐতিহ্যৰ সংগী",
            "lus" to "Hriatna Hloh Enkawlna & Hnam Rohlu",
            "kha" to "Ka Jingiarap Jingkynmaw & Ka Riti Tynrai",
            "mni" to "লৈখিদ্রবা ৱাখল্লোন য়েংশিনবা অমসুং চৎনবী সংগী",
            "nag" to "Dementia Care & Hami Khan laga Heritage Sathi"
        ),
        "auth_features_sub" to mapOf(
            "en" to "AI Neuro-Telemetry • Cultural Reminiscence • Safe GPS Radar",
            "hi" to "एआई न्यूरो-टेलीमेट्री • सांस्कृतिक स्मृति • सुरक्षित जीपीएस रडार",
            "as" to "এআই নিউৰো-টেলিমেট্ৰি • সাংস্কৃতিক স্মৃতি • সুৰক্ষিত জিপিএছ ৰাডাৰ",
            "lus" to "AI Rilru Tehna • Hnam Hriatrengna • Himna GPS Radar",
            "kha" to "AI Jingmut Telemetry • Jingkynmaw Riti • Shngiam GPS Radar",
            "mni" to "AI ন্যুৰো-তেলিমেট্রি • চৎনবী নীংশিংবা • সেফ GPS ৰাদাৰ",
            "nag" to "AI Neuro Telemetry • Cultural Yaad • Safe GPS Radar"
        ),
        "auth_badge" to mapOf(
            "en" to "REGIONAL HERITAGE & COGNITIVE SENTINEL",
            "hi" to "क्षेत्रीय विरासत एवं संज्ञानात्मक प्रहरी",
            "as" to "আঞ্চলিক ঐতিহ্য আৰু বোধশক্তিৰ প্ৰহৰী",
            "lus" to "HNAM ROHLU & RILRU VENHIMTU",
            "kha" to "KA RITI TYNRAI & KA JINGAP JINGMUT",
            "mni" to "চৎনবী হেরিতেজ অমসুং ৱাখল্লোন ঙাকপীবী",
            "nag" to "HERITAGE & COGNITIVE SENTINEL"
        ),
        "auth_badge_desc" to mapOf(
            "en" to "Rooted in rich regional traditions and clinical neuroscience. Preserving cherished memories, family connections, and 24/7 patient boundary safety.",
            "hi" to "समृद्ध क्षेत्रीय परंपराओं और क्लिनिकल न्यूरोसाइंस पर आधारित। संजोई हुई यादों, पारिवारिक संबंधों और २४/७ सुरक्षा का संरक्षण।",
            "as" to "সমৃদ্ধ আঞ্চলিক পৰম্পৰা আৰু ক্লিনিকেল নিউৰোবিজ্ঞানত শিপোৱা। সোঁৱৰণি, পাৰিবাৰিক সম্পৰ্ক আৰু ২৪/৭ নিৰাপত্তাৰ সংৰক্ষণ।",
            "lus" to "Hnam zia leh thluak thiamna hmanga siam. Hriatrengna hlu, chhungkua leh himna 24/7 a venhimna.",
            "kha" to "Buh ha ka riti tynrai bad ka neuroscience. Ri ia ki jingkynmaw kordor, ka jingialong kawei bad ka jingshngiam 24/7.",
            "mni" to "লৈবাক্কী চৎনবী অমসুং ক্লিনিকেল ন্যুরোসাইন্সতা য়ুম্ফম ওইবা। নীংশিংবা, ইমুংগী মরী অমসুং ২৪/৭ সেফটি ঙাকপা।",
            "nag" to "Apun laga tradition aru clinical neuroscience logote bonaishe. Sob bhal yaad, poribar aru 24/7 patient safety rakhibo."
        ),
        "auth_chip_roots" to mapOf(
            "en" to "🌸 Regional Roots",
            "hi" to "🌸 क्षेत्रीय जड़ें",
            "as" to "🌸 আঞ্চলিক শিপা",
            "lus" to "🌸 Hnam Bulthut",
            "kha" to "🌸 Ki Tynrai Riti",
            "mni" to "🌸 চৎনবীগী মরম",
            "nag" to "🌸 Nijor Roots"
        ),
        "auth_chip_games" to mapOf(
            "en" to "🧠 Neuro-Games",
            "hi" to "🧠 न्यूरो-खेल",
            "as" to "🧠 নিউৰো-খেল",
            "lus" to "🧠 Thluak Infiamna",
            "kha" to "🧠 Ki Jingialehkai Jingmut",
            "mni" to "🧠 ন্যুরো-শান্নবা",
            "nag" to "🧠 Dimag laga Khel"
        ),
        "auth_chip_gps" to mapOf(
            "en" to "🛡️ GPS Sentinel",
            "hi" to "🛡️ जीपीएस प्रहरी",
            "as" to "🛡️ জিপিএছ প্ৰহৰী",
            "lus" to "🛡️ GPS Venhimtu",
            "kha" to "🛡️ Ka Jingiada GPS",
            "mni" to "🛡️ GPS ঙাকপীবী",
            "nag" to "🛡️ GPS Sentinel"
        ),
        "auth_chip_home" to mapOf(
            "en" to "🏡 Take Me Home",
            "hi" to "🏡 मुझे घर ले चलो",
            "as" to "🏡 মোক ঘৰলৈ লৈ ব'লক",
            "lus" to "🏡 In Lam Pan",
            "kha" to "🏡 Leit Sha Ing",
            "mni" to "🏡 য়ুমদা পুখিনবিয়ু",
            "nag" to "🏡 Ghor te Loi Jabi"
        ),
        "auth_sign_in" to mapOf(
            "en" to "Sign In",
            "hi" to "साइन इन करें",
            "as" to "ছাইন ইন কৰক",
            "lus" to "Lut Rawh",
            "kha" to "Rung Mynta",
            "mni" to "সাইন্ ইন তৌবিয়ু",
            "nag" to "Sign In Kuribi"
        ),
        "auth_1tap" to mapOf(
            "en" to "1-Tap Sign In",
            "hi" to "१-टैप साइन इन",
            "as" to "১-টিপ ছাইন ইন",
            "lus" to "Hmet Vawikhat Lut",
            "kha" to "Rung Shi Kyntuit",
            "mni" to "১-নম্বা সাইন্ ইন",
            "nag" to "1-Tap Sign In"
        ),
        "auth_continue_google" to mapOf(
            "en" to "Continue with Google",
            "hi" to "Google के साथ जारी रखें",
            "as" to "Google ৰ সৈতে আগবাঢ়ক",
            "lus" to "Google hmanga chhunzawm",
            "kha" to "Iaibteng bad Google",
            "mni" to "Google গা লোয়ননা চৎখিবিয়ু",
            "nag" to "Google logote Continue Kuribi"
        ),
        "auth_choose_another" to mapOf(
            "en" to "Choose Another Account",
            "hi" to "दूसरा खाता चुनें",
            "as" to "অন্য একাউণ্ট বাছক",
            "lus" to "Account Dang Thlang Rawh",
            "kha" to "Jied da ka Account Kawei",
            "mni" to "অতোপ্পা একাউন্ট খনবিয়ু",
            "nag" to "Aro Ekta Account Chunu"
        ),
        "auth_remember_device" to mapOf(
            "en" to "Remember this Device (Stay Signed In)",
            "hi" to "इस डिवाइस को याद रखें (लॉग इन रहें)",
            "as" to "এই ডিভাইচটো মনত ৰাখক (লগ ইন হৈ থাকক)",
            "lus" to "He Khawl hi Hre reng rawh (Lut reng rawh)",
            "kha" to "Kynmaw ia kane ka Device (Rung beit)",
            "mni" to "মসিগী দিভাইস নীংশিংবিয়ু (লগ ইন লৈহন্নবা)",
            "nag" to "Etu Device Yaad Rakhibi (Always Login)"
        ),
        "auth_quick_access" to mapOf(
            "en" to "⚡ Quick Device Access (Continue as This Device)",
            "hi" to "⚡ त्वरित डिवाइस प्रवेश (इस डिवाइस के रूप में जारी रखें)",
            "as" to "⚡ দ্ৰুত ডিভাইচ প্ৰৱেশ (এই ডিভাইচ হিচাপে আগবাঢ়ক)",
            "lus" to "⚡ Khawl Hman Zung Zungna",
            "kha" to "⚡ Ka Jingrung Kloi",
            "mni" to "⚡ য়াংনা দিভাইস চংবা",
            "nag" to "⚡ Jaldi Device Access"
        ),
        "auth_signing_in" to mapOf(
            "en" to "Signing In...",
            "hi" to "साइन इन हो रहा है...",
            "as" to "ছাইন ইন হৈ আছে...",
            "lus" to "Lut mek...",
            "kha" to "Dang rung...",
            "mni" to "সাইন্ ইন তৌরি...",
            "nag" to "Sign In Hoi Ase..."
        ),

        // Memory Vault Sub-Panel Strings
        "mv_next_memory" to mapOf(
            "en" to "Next Cherished Memory",
            "hi" to "अगली अनमोल स्मृति",
            "as" to "পৰৱৰ্তী মৰমৰ স্মৃতি",
            "lus" to "Hriatrengna Dang Hmeh",
            "kha" to "Ka Jingkynmaw Kordor Bud",
            "mni" to "মথংগী নীংশিংবা",
            "nag" to "Dusra Bhal Yaad"
        ),
        "mv_default_themes" to mapOf(
            "en" to "Choose from 6 Default Themes:",
            "hi" to "६ पूर्वनिर्धारित थीमों में से चुनें:",
            "as" to "৬টা অবিকল্পিত বিষয়ৰ পৰা বাছক:",
            "lus" to "Thupui 6 Aṭangin Thlang Rawh:",
            "kha" to "Jied na ki 6 tylli ki Theme:",
            "mni" to "থিম ৬ তগী খনবিয়ু:",
            "nag" to "6 ta Theme para Chunu:"
        ),
        "mv_add_first" to mapOf(
            "en" to "Add First Family Memory",
            "hi" to "पहली पारिवारिक स्मृति जोड़ें",
            "as" to "প্ৰথম পাৰিবাৰিক স্মৃতি যোগ কৰক",
            "lus" to "Chhungkaw Hriatrengna Hmasa Dah Rawh",
            "kha" to "Buh ia ka Jingkynmaw Kynhun Nyngkong",
            "mni" to "অহানবা ইমুংগী নীংশিংবা হাপচিনবিয়ু",
            "nag" to "Pehla Family Yaad Dalibi"
        ),
        "mv_upload_title" to mapOf(
            "en" to "Upload Family Memory Question",
            "hi" to "पारिवारिक स्मृति प्रश्न जोड़ें",
            "as" to "পাৰিবাৰিক স্মৃতি প্ৰশ্ন আপলোড কৰক",
            "lus" to "Chhungkaw Zawhna Dahna",
            "kha" to "Buh Jingkylli Jingkynmaw",
            "mni" to "ইমুংগী নীংশিংবা ৱাহং হাপচিনবিয়ু",
            "nag" to "Family Yaad Sawaal Dalibi"
        ),
        "mv_select_theme" to mapOf(
            "en" to "Select Default Theme:",
            "hi" to "थीम का चयन करें:",
            "as" to "বিষয় বাছক:",
            "lus" to "Thupui Thlang Rawh:",
            "kha" to "Jied ia ka Theme:",
            "mni" to "থিম খনবিয়ু:",
            "nag" to "Theme Chunu:"
        ),
        "mv_title_label" to mapOf(
            "en" to "Memory Title",
            "hi" to "स्मृति शीर्षक",
            "as" to "স্মৃতিৰ শিৰোনাম",
            "lus" to "Hriatrengna Hming",
            "kha" to "Kyrteng Jingkynmaw",
            "mni" to "নীংশিংবগী মমিং",
            "nag" to "Yaad laga Naam"
        ),
        "mv_title_hint" to mapOf(
            "en" to "e.g. Grandma's Favorite Dish",
            "hi" to "उदा. दादी का पसंदीदा व्यंजन",
            "as" to "উদাহৰণ: আইতাৰ প্ৰিয় খাদ্য",
            "lus" to "Entirnan: Pi chawhmeh duh ber",
            "kha" to "Nuksa: Ka jingbam ba bang ka Mei-rad",
            "mni" to "খুদম: ইবেম্মাগী নুংসিবা চিঞ্জাক",
            "nag" to "Dadi laga Bhal Khana"
        ),
        "mv_question_label" to mapOf(
            "en" to "Memory Question",
            "hi" to "स्मृति प्रश्न",
            "as" to "স্মৃতিৰ প্ৰশ্ন",
            "lus" to "Hriatrengna Zawhna",
            "kha" to "Jingkylli Jingkynmaw",
            "mni" to "নীংশিংবা ৱাহং",
            "nag" to "Yaad laga Sawaal"
        ),
        "mv_question_hint" to mapOf(
            "en" to "e.g. Which spice does Grandma add to tea?",
            "hi" to "उदा. दादी चाय में कौन सा मसाला डालती हैं?",
            "as" to "উদাহৰণ: আইতাই চাহাত কি মছলা দিয়ে?",
            "lus" to "Entirnan: Pi-in thingpuia a telh ṭhin?",
            "kha" to "Nuksa: Kaei ba ka Mei-rad ka thep ha ka sha?",
            "mni" to "খুদম: ইবেম্মান চা দা করি মশলা হাপই?",
            "nag" to "Dadi cha te ki masala dale?"
        ),
        "mv_cue_label" to mapOf(
            "en" to "Helpful Hint / Cue",
            "hi" to "मददगार संकेत / हिंट",
            "as" to "সহায়কাৰী ইঙ্গিত",
            "lus" to "Puihna / Hriattirna",
            "kha" to "Jingsneng Yarap",
            "mni" to "মতেং পাংবা ইন্ত",
            "nag" to "Modot laga Hint"
        ),
        "mv_cue_hint" to mapOf(
            "en" to "e.g. Fresh green leaves from garden",
            "hi" to "उदा. बगीचे से ताज़ी हरी पत्तियां",
            "as" to "উদাহৰণ: বাৰীৰ সতেজ সেউজীয়া পাত",
            "lus" to "Entirnan: Hwanna hnah hring thar",
            "kha" to "Nuksa: Ki sla ba jyrngam na kper",
            "mni" to "খুদম: লৈকোলদগী অহিংবা মনা",
            "nag" to "Bagan laga taza patta"
        ),
        "mv_options_title" to mapOf(
            "en" to "Multiple Choice Options (Select correct answer):",
            "hi" to "विकल्प चुनें (सही उत्तर पर टैप करें):",
            "as" to "বিকল্প বাছক (শুদ্ধ উত্তৰত টিপক):",
            "lus" to "Thlan turte (A dik ber hmet rawh):",
            "kha" to "Ki jingjied (Kyntuit ia kaba dei):",
            "mni" to "অপশনশিং (চুম্বা পাউখুম খনবিয়ু):",
            "nag" to "Options (Sahi uttar select kuribi):"
        ),
        "mv_save" to mapOf(
            "en" to "Save to Vault",
            "hi" to "वॉल्ट में सुरक्षित करें",
            "as" to "ভঁৰালত সংৰক্ষণ কৰক",
            "lus" to "Dah Ṭha Rawh",
            "kha" to "Kynshew ha ka Vault",
            "mni" to "ভোল্টতা সেভ তৌবিয়ু",
            "nag" to "Vault te Save Kuribi"
        ),
        "mv_cancel" to mapOf(
            "en" to "Cancel",
            "hi" to "रद्द करें",
            "as" to "বাতিল কৰক",
            "lus" to "Sut Leh",
            "kha" to "Pynsangeh",
            "mni" to "লেপহন্বিয়ু",
            "nag" to "Cancel"
        ),

        // Safety Alerts Sub-Panel Strings
        "safety_sos" to mapOf(
            "en" to "EMERGENCY SOS",
            "hi" to "आपातकालीन एसओएस (SOS)",
            "as" to "জৰুৰীকালীন এছ.অ'.এছ (SOS)",
            "lus" to "CHHIATRUP SOS",
            "kha" to "JINGKYRDUH SOS",
            "mni" to "ইমর্জেন্সী SOS",
            "nag" to "EMERGENCY SOS"
        ),
        "safety_geofence_title" to mapOf(
            "en" to "Safe Zone Boundary",
            "hi" to "सुरक्षित क्षेत्र सीमा",
            "as" to "সুৰক্ষিত মণ্ডলৰ সীমা",
            "lus" to "Hmun Him Ramri",
            "kha" to "U Pud Ka Jingshngiam",
            "mni" to "সেফ জোন ঙমখৈ",
            "nag" to "Safe Zone Boundary"
        ),
        "safety_geofence_sub" to mapOf(
            "en" to "Active 500m Home Geofence",
            "hi" to "सक्रिय ५०० मीटर गृह जियोफेंस",
            "as" to "সক্ৰিয় ৫০০ মিটাৰ ঘৰৰ জিপিএছ সীমা",
            "lus" to "In chhehvel 500m Himna",
            "kha" to "500m Sawdong Ing ba Trei Kam",
            "mni" to "৫০০ মিটর য়ুমগী সেফ জোন এক্টিভ",
            "nag" to "500m Ghor Safe Zone Chalu Ase"
        ),
        "safety_geofence_desc" to mapOf(
            "en" to "Your GPS location is being continuously monitored by your family guardian. If you step outside your designated boundary, your family is alerted immediately.",
            "hi" to "आपके परिवार द्वारा आपके जीपीएस स्थान की निरंतर निगरानी की जा रही है। यदि आप अपनी सीमा से बाहर जाते हैं, तो परिवार को तुरंत सूचित किया जाता है।",
            "as" to "আপোনাৰ জিপিএছ অৱস্থান পৰিয়ালে নিৰন্তৰ নিৰীক্ষণ কৰি আছে। নিৰ্ধাৰিত সীমাৰ বাহিৰলৈ গ'লে পৰিয়ালক তৎক্ষণাত সতৰ্ক কৰা হ'ব।",
            "lus" to "I GPS awmna chu i chhungten an thlithlai reng a ni. I ramri pawn i chhuah chuan i chhungte hriattir nghal an ni ang.",
            "kha" to "Ka jaka GPS jong phi la peitngor da ka kynhun iing. Lada phi mih shabar, kin ioh jingtip mar-ia-mar.",
            "mni" to "অদোমগী GPS মফম ইমুংনা লেপ্তনা য়েংশিল্লি। ঙমখৈগী মপান থোক্লবদি ইমুংদা তৎক্ষণাৎ খঙহনগনি।",
            "nag" to "Apun laga GPS location poribar manu saikina ase. Boundary bahar jale poribar manu logote turant alert jabo."
        ),
        "safety_med_id" to mapOf(
            "en" to "Emergency Medical ID",
            "hi" to "आपातकालीन मेडिकल पहचान",
            "as" to "জৰুৰীকালীন চিকিৎসা পৰিচয়",
            "lus" to "Chhiatrup Damdawi ID",
            "kha" to "Ka Medical ID Jingkyrduh",
            "mni" to "ইমর্জেন্সী মেদিকেল ID",
            "nag" to "Emergency Medical ID"
        ),
        "safety_patient_name" to mapOf(
            "en" to "Patient Name:",
            "hi" to "मरीज़ का नाम:",
            "as" to "ৰোগীৰ নাম:",
            "lus" to "Damin Hming:",
            "kha" to "Kyrteng Nongpang:",
            "mni" to "অনাবাগী মমিং:",
            "nag" to "Patient laga Naam:"
        ),
        "safety_condition" to mapOf(
            "en" to "Condition:",
            "hi" to "चिकित्सीय स्थिति:",
            "as" to "শাৰীৰিক অৱস্থা:",
            "lus" to "Natna Dinhmun:",
            "kha" to "Ka Jinglong:",
            "mni" to "লাইনাগী ফীভম:",
            "nag" to "Condition:"
        ),
        "safety_condition_desc" to mapOf(
            "en" to "Memory & Alzheimer's Care Assistance",
            "hi" to "स्मृति एवं अल्जाइमर देखभाल सहायता",
            "as" to "স্মৃতি আৰু এলঝাইমাৰ যত্ন সাহাৰ্য",
            "lus" to "Hriatna Hloh Enkawlna",
            "kha" to "Jingiarap Jingkynmaw & Alzheimer's",
            "mni" to "নীংশিংবা অমসুং আলঝাইমার য়েংশিনবা",
            "nag" to "Memory & Alzheimer Care Modot"
        ),
        "safety_caregiver" to mapOf(
            "en" to "Caregiver:",
            "hi" to "देखभालकर्ता:",
            "as" to "শুশ্ৰূষাকাৰী:",
            "lus" to "Enkawltu:",
            "kha" to "Nongsumar:",
            "mni" to "য়েংশিনবীবী:",
            "nag" to "Caregiver:"
        ),
        "safety_address" to mapOf(
            "en" to "Home Address:",
            "hi" to "घर का पता:",
            "as" to "ঘৰৰ ঠিকনা:",
            "lus" to "In Awmna:",
            "kha" to "Ka Jaka Ing:",
            "mni" to "য়ুমগী লৈফম:",
            "nag" to "Ghor laga Address:"
        ),
        "safety_wellness" to mapOf(
            "en" to "Daily Wellness Checklist",
            "hi" to "दैनिक स्वास्थ्य चेकलिस्ट",
            "as" to "দৈনন্দিন স্বাস্থ্য তালিকা",
            "lus" to "Ni Tin Hriselna Enna",
            "kha" to "Ka Jingpeit Ka Koit Ka Khiah Minta Ka Sngi",
            "mni" to "নোংমগী হকশেল চেক্লিষ্ট",
            "nag" to "Roj laga Wellness List"
        ),
        "safety_meds" to mapOf(
            "en" to "Daily Medication",
            "hi" to "दैनिक दवाइयाँ",
            "as" to "দৈনিক ঔষধ",
            "lus" to "Ni Tin Damdawi",
            "kha" to "Ki Dawai Minta Ka Sngi",
            "mni" to "নোংমগী হিদাক-লাংথক",
            "nag" to "Roj laga Dawaai"
        ),
        "safety_hydration" to mapOf(
            "en" to "Hydration Check",
            "hi" to "पानी / जलयोजन जाँच",
            "as" to "পানী খোৱাৰ নিৰীক্ষণ",
            "lus" to "Tui In Lam Enna",
            "kha" to "Jingdih Um",
            "mni" to "ঈশিং থকপগী চেকিং",
            "nag" to "Paani Khawa Check"
        ),
        "safety_dialog_title" to mapOf(
            "en" to "Emergency Contact & Home Safe Location",
            "hi" to "आपातकालीन संपर्क एवं घर का सुरक्षित स्थान",
            "as" to "জৰুৰীকালীন যোগাযোগ আৰু ঘৰৰ সুৰক্ষিত স্থান",
            "lus" to "Chhiatrup Biak Pawhna & In Hmun",
            "kha" to "Jingtip Jingkyrduh & Ka Jaka Ing",
            "mni" to "ইমর্জেন্সী কন্তেক্ত অমসুং য়ুমগী মফম",
            "nag" to "Emergency Contact & Ghor Safe Location"
        ),
        "safety_caregiver_name" to mapOf(
            "en" to "Caregiver Name",
            "hi" to "देखभालकर्ता का नाम",
            "as" to "শুশ্ৰূষাকাৰীৰ নাম",
            "lus" to "Enkawltu Hming",
            "kha" to "Kyrteng Nongsumar",
            "mni" to "য়েংশিনবীবগী মমিং",
            "nag" to "Caregiver laga Naam"
        ),
        "safety_caregiver_phone" to mapOf(
            "en" to "Caregiver Phone Number",
            "hi" to "देखभालकर्ता का फ़ोन नंबर",
            "as" to "শুশ্ৰূষাকাৰীৰ ফোন নম্বৰ",
            "lus" to "Enkawltu Phone Number",
            "kha" to "Phone Number Nongsumar",
            "mni" to "য়েংশিনবীবগী ফোন নম্বর",
            "nag" to "Caregiver Phone Number"
        ),
        "safety_home_address_label" to mapOf(
            "en" to "Home Street Address or Landmark",
            "hi" to "घर का पता या लैंडमार्क",
            "as" to "ঘৰৰ ঠিকনা বা পৰিচিত স্থান",
            "lus" to "In Awmna / Hriat Awlsamna",
            "kha" to "Ka Jaka Ing Lanei Ka Dak",
            "mni" to "য়ুমগী লম্বীগী মমিং নত্রগা লেন্দমার্ক",
            "nag" to "Ghor Address ki Landmark"
        ),

        // Cognitive Health Sub-Panel Strings
        "health_cps_score" to mapOf(
            "en" to "CPS SCORE",
            "hi" to "सीपीएस स्कोर",
            "as" to "চিপিএছ স্ক'ৰ",
            "lus" to "CPS Tehna",
            "kha" to "Ka Jingthew CPS",
            "mni" to "CPS স্কোর",
            "nag" to "CPS Score"
        ),
        "health_cog_age" to mapOf(
            "en" to "Cognitive Age",
            "hi" to "संज्ञानात्मक आयु",
            "as" to "মানসিক বয়স",
            "lus" to "Rilru Kum",
            "kha" to "Ka Rta Jingmut",
            "mni" to "ৱাখল্লোনগী চহী",
            "nag" to "Dimag laga Umor"
        ),
        "health_bio_age" to mapOf(
            "en" to "Biological Age",
            "hi" to "जैविक आयु",
            "as" to "দৈহিক বয়স",
            "lus" to "Taksa Kum",
            "kha" to "Ka Rta Met",
            "mni" to "হকচাংগী চহী",
            "nag" to "Asli Umor"
        ),
        "health_subdomains" to mapOf(
            "en" to "Clinical Sub-Domain Performance",
            "hi" to "क्लिनिकल उप-क्षेत्रीय प्रदर्शन",
            "as" to "ক্লিনিকেল উপ-ক্ষেত্ৰীয় ফলাফল",
            "lus" to "Thluak Peng Hrang Hrang Tehna",
            "kha" to "Ka Jingtrei Kam Ki Bynta Jingmut",
            "mni" to "ক্লিনিকেল তোঙান-তোঙানবা পরফোর্মেন্স",
            "nag" to "Clinical Sub-Domain Performance"
        ),
        "health_forecast" to mapOf(
            "en" to "Trajectory Forecast",
            "hi" to "प्रक्षेपवक्र पूर्वानुमान",
            "as" to "ভৱিষ্যত মানসিক দিশ",
            "lus" to "Rilru Dinhmun Hmathlir",
            "kha" to "Ka Jingiaid Ka Jingmut",
            "mni" to "তুংলমচৎকী ৱাখল্লোন মওং",
            "nag" to "Aage laga Forecast"
        ),
        "health_30days" to mapOf(
            "en" to "Projected 30 Days",
            "hi" to "३० दिनों का अनुमान",
            "as" to "৩০ দিনৰ পূৰ্বানুমান",
            "lus" to "Ni 30 Hmathlir",
            "kha" to "30 Sngi Ban Wan",
            "mni" to "নুমিৎ ৩০ গী প্রজেক্সন",
            "nag" to "30 Din laga Projection"
        ),
        "health_90days" to mapOf(
            "en" to "Projected 90 Days",
            "hi" to "९० दिनों का अनुमान",
            "as" to "৯০ দিনৰ পূৰ্বানুমান",
            "lus" to "Ni 90 Hmathlir",
            "kha" to "90 Sngi Ban Wan",
            "mni" to "নুমিৎ ৯০ গী প্রজেক্সন",
            "nag" to "90 Din laga Projection"
        ),

        // Beacon Tracker Sub-Panel Strings
        "beacon_gps_high_acc" to mapOf(
            "en" to "Tap here to turn on Google High-Accuracy GPS with one tap.",
            "hi" to "एक टैप में Google उच्च-सटीक जीपीएस चालू करने के लिए यहाँ टैप करें।",
            "as" to "এক টিপত Google উচ্চ-সঠিকতা জিপিএছ অন কৰিবলৈ ইয়াত টিপক।",
            "lus" to "Hmet vawikhatin GPS fiah zawk on rawh.",
            "kha" to "Kyntuit ban plie ia ka GPS ba biang bha.",
            "mni" to "অকনবা একুরেসি GPS হৌনবা মসিদা নম্বিয়ু।",
            "nag" to "Ete dababi High-Accuracy GPS chalu kuribo karone."
        ),
        "beacon_bg_perm_title" to mapOf(
            "en" to "Background Permission Needed",
            "hi" to "पृष्ठभूमि अनुमति आवश्यक",
            "as" to "পশ্চাৎভূমি অনুমতিৰ প্ৰয়োজন",
            "lus" to "Hnunglam Hman Phalsak A Ngai",
            "kha" to "Donkam Jingbit Ban Trei Kam Sha Lyndet",
            "mni" to "বেকগ্রাউন্দ অয়াবা দরকার ওইরি",
            "nag" to "Background Permission Lage"
        ),
        "beacon_bg_perm_sub" to mapOf(
            "en" to "Tap to set 'Allow all the time' for continuous 24/7 tracking.",
            "hi" to "२४/७ निरंतर ट्रैकिंग हेतु 'हर समय अनुमति दें' सेट करने के लिए टैप करें।",
            "as" to "২৪/৭ নিৰন্তৰ নিৰীক্ষণৰ বাবে 'সদায় অনুমতি দিয়ক' বাছক।",
            "lus" to "24/7 vil reng nan 'Allow all the time' thlang rawh.",
            "kha" to "Jied 'Allow all the time' ban ioh peit 24/7.",
            "mni" to "২৪/৭ ত্রাকিংগীদমক 'Allow all the time' খনবিয়ু।",
            "nag" to "24/7 tracking karone 'Allow all the time' rakhabi."
        ),
        "beacon_bg_perm_allowed" to mapOf(
            "en" to "Background Location: Allowed All the Time",
            "hi" to "पृष्ठभूमि स्थान: हर समय अनुमत",
            "as" to "পশ্চাৎভূমি স্থান: সদায় অনুমতি প্ৰদত্ত",
            "lus" to "Hnunglam Awmna: Phalsak Reng A Ni",
            "kha" to "Ka Jaka Sha Lyndet: Shah Baroh Ka Por",
            "mni" to "বেকগ্রাউন্দ মফম: মতম পুম্বদা য়ারি",
            "nag" to "Background Location: Sob Koste Allowed Ase"
        ),
        "beacon_live_badge" to mapOf(
            "en" to "LIVE",
            "hi" to "लाइव",
            "as" to "লাইভ",
            "lus" to "LIVE",
            "kha" to "MYNTA",
            "mni" to "লাইভ",
            "nag" to "LIVE"
        ),
        "beacon_acquiring" to mapOf(
            "en" to "Acquiring exact GPS satellite fix...",
            "hi" to "सटीक जीपीएस उपग्रह सिग्नल प्राप्त किया जा रहा है...",
            "as" to "সঠিক জিপিএছ উপগ্ৰহ সংকেত লোৱা হৈছে...",
            "lus" to "GPS satellite lak mek a ni...",
            "kha" to "Dang wad ia ka dak satellite GPS...",
            "mni" to "GPS সেতেলাইত সিগনেল লৌরি...",
            "nag" to "Exact GPS satellite dhori ase..."
        ),
        "beacon_start_hint" to mapOf(
            "en" to "Start broadcasting to view live GPS coordinates and send real-time pings to caregivers.",
            "hi" to "लाइव जीपीएस निर्देशांक देखने और देखभालकर्ताओं को सिग्नल भेजने के लिए प्रसारण शुरू करें।",
            "as" to "লাইভ জিপিএছ চাবলৈ আৰু পৰিয়াললৈ সংকেত পঠাবলৈ সম্প্ৰচাৰ আৰম্ভ কৰক।",
            "lus" to "GPS hmuh nan leh enkawltute hnena thawn nan thawn tan rawh.",
            "kha" to "Sdang ban phah ia ka jaka sha ki nongsumar.",
            "mni" to "লাইভ GPS মফম য়েংনবা অমসুং ইমুংদা সিগনেল থানবা ব্রোদকাস্ত হৌবিয়ু।",
            "nag" to "Live GPS coordinates saikina caregiver ke pathabole broadcast chalu kuribi."
        ),
        "beacon_continue" to mapOf(
            "en" to "Continue",
            "hi" to "जारी रखें",
            "as" to "আগবাঢ়ক",
            "lus" to "Chhunzawm Rawh",
            "kha" to "Iaibteng",
            "mni" to "চৎখিবিয়ু",
            "nag" to "Aage Jaabi"
        ),
        "beacon_later" to mapOf(
            "en" to "Later",
            "hi" to "बाद में",
            "as" to "পাছত",
            "lus" to "Nakinah",
            "kha" to "Hadien",
            "mni" to "তুংদা",
            "nag" to "Pichete"
        ),

        // Brain Exercises Sub-Panel Strings
        "game1_matched_status" to mapOf(
            "en" to "pairs matched",
            "hi" to "जोड़े मिले",
            "as" to "যোৰ মিলিল",
            "lus" to "inmil tawh",
            "kha" to "ki jingsa ba iadei",
            "mni" to "পেয়ার য়ানরে",
            "nag" to "jodi milise"
        ),
        "game2_watch_glow" to mapOf(
            "en" to "Watch the pattern glow...",
            "hi" to "पैटर्न की चमक को ध्यान से देखें...",
            "as" to "প্ৰতিৰূপৰ উজ্বলতা লক্ষ্য কৰক...",
            "lus" to "A eng dan hi ngun takin en rawh...",
            "kha" to "Peit thuh ia ka jingthaba...",
            "mni" to "প্যাতর্নগী মঙাল য়েংবিয়ু...",
            "nag" to "Pattern chowa bhalte..."
        ),
        "game2_your_turn_prompt" to mapOf(
            "en" to "Your turn! Tap the pads in order",
            "hi" to "आपकी बारी! उसी क्रम में पैड पर टैप करें",
            "as" to "আপোনাৰ পাল! সেই ক্ৰমত পেডত টিপক",
            "lus" to "I hun ve le! A indawtin hmet rawh",
            "kha" to "Ka pali jong phi! Kyntuit beit",
            "mni" to "অদোমগী তাঞ্জা! চপ মান্নবা মতুং ইন্না নম্বিয়ু",
            "nag" to "Apun laga baari! Order te dababi"
        ),
        "game2_level_cleared_msg" to mapOf(
            "en" to "Level Cleared! Next level...",
            "hi" to "स्तर पूरा हुआ! अगला स्तर...",
            "as" to "স্তৰ সমাপ্ত! পৰৱৰ্তী স্তৰ...",
            "lus" to "I thiam e! A dawt leh...",
            "kha" to "La dep! Ka kylla bud...",
            "mni" to "লেভেল লোয়রে! মথংগী লেভেল...",
            "nag" to "Level Kothom! Dusra level..."
        ),
        "game2_missed_msg" to mapOf(
            "en" to "Missed! Watch again...",
            "hi" to "चूक गए! दोबारा ध्यान से देखें...",
            "as" to "ভুল হ'ল! পুনৰ চাওক...",
            "lus" to "I thelh e! En ṭha leh rawh...",
            "kha" to "Bakla! Peit biang...",
            "mni" to "সোইরে! অমুক্কা য়েংবিয়ু...",
            "nag" to "Galti hoise! Phir saabi..."
        ),
        "game3_tap_ink" to mapOf(
            "en" to "Tap the INK COLOR",
            "hi" to "स्याही का रंग चुनें",
            "as" to "চিঞাহীৰ ৰং বাছক",
            "lus" to "A Rawng Dik Hmet Rawh",
            "kha" to "Kyntuit ia ka Rong",
            "mni" to "মচুগী মচু নম্বিয়ু",
            "nag" to "INK COLOR ke dababi"
        ),
        "game3_select_prompt" to mapOf(
            "en" to "Select the INK COLOR:",
            "hi" to "स्याही का रंग चुनें:",
            "as" to "চিঞাহীৰ ৰং বাছক:",
            "lus" to "A rawng dik thlang rawh:",
            "kha" to "Jied ia ka Rong:",
            "mni" to "মচুগী মচু খনবিয়ু:",
            "nag" to "INK COLOR select kuribi:"
        ),
        "game4_next_prompt" to mapOf(
            "en" to "Next",
            "hi" to "अगला",
            "as" to "পৰৱৰ্তী",
            "lus" to "A dawt",
            "kha" to "Bud",
            "mni" to "মথং",
            "nag" to "Next"
        ),

        // Voice Guidance Panel
        "voice_preview_title" to mapOf(
            "en" to "Voice Encouragement Preview",
            "hi" to "आवाज़ प्रोत्साहन पूर्वावलोकन",
            "as" to "কণ্ঠ উৎসাহ পূৰ্বদৰ্শন",
            "lus" to "Aw Puihna Ngaihthlakna",
            "kha" to "Ka Sur Kyntu Kynpham",
            "mni" to "খোল্লাক ইথিল য়েংবা",
            "nag" to "Awaaz Modot Preview"
        ),
        "voice_preview_btn" to mapOf(
            "en" to "Hear Encouraging Voice",
            "hi" to "प्रोत्साहक आवाज़ सुनें",
            "as" to "উৎসাহজনক মাত শুনক",
            "lus" to "Aw Ngaihnawm Ngaihtla Rawh",
            "kha" to "Sngap ia ka Sur",
            "mni" to "ইথিল পীবী খোঞ্জেল তাবিয়ু",
            "nag" to "Awaaz Sunibi"
        ),

        // App header & subtitle
        "app_title" to mapOf(
            "en" to "Smaran",
            "hi" to "स्मरण (Smaran)",
            "as" to "স্মৰণ (Smaran)",
            "lus" to "Smaran Hriatna",
            "kha" to "Smaran Jingkynmaw",
            "mni" to "স্মরণ (Smaran)",
            "nag" to "Smaran Dimag"
        ),
        "beacon_live" to mapOf(
            "en" to "Live Beacon Active",
            "hi" to "लाइव बीकन सक्रिय",
            "as" to "লাইভ বিকন সক্ৰিয়",
            "lus" to "Location Thawn Mek",
            "kha" to "Ka Jingithuh Shai",
            "mni" to "লাইভ বিকন এক্টিভ",
            "nag" to "Live Beacon Chalu Ase"
        ),
        "beacon_standby" to mapOf(
            "en" to "Beacon Standby",
            "hi" to "बीकन स्टैंडबाय",
            "as" to "বিকন অপক্ষাৰত",
            "lus" to "Inring Reirawh",
            "kha" to "Pynsngap Shuwa",
            "mni" to "বিকন লেপ্লি",
            "nag" to "Beacon Standby Ase"
        ),

        // Tile 1: GPS Beacon
        "tile_gps_title" to mapOf(
            "en" to "GPS Beacon",
            "hi" to "जीपीएस बीकन",
            "as" to "জি.পি.এছ. বিকন",
            "lus" to "GPS Hmun Zawnna",
            "kha" to "Ka Jingbuh Hmun GPS",
            "mni" to "GPS মফম তাকপা",
            "nag" to "GPS Beacon Jagah"
        ),
        "tile_gps_sub_broadcasting" to mapOf(
            "en" to "Transmitting Live",
            "hi" to "लाइव प्रसारण चालू",
            "as" to "লাইভ প্ৰেৰণ চলিছে",
            "lus" to "Thawn Mek A Ni",
            "kha" to "Phah Jingtip Mynta",
            "mni" to "লাইভ ত্রান্সমিৎ তৌরি",
            "nag" to "Live Bheji Ase"
        ),
        "tile_gps_sub_standby" to mapOf(
            "en" to "Tap to Broadcast",
            "hi" to "प्रसारण हेतु टैप करें",
            "as" to "প্ৰেৰণ কৰিবলৈ টিপক",
            "lus" to "Thawn Tan Nan Hmet",
            "kha" to "Kyntuit Ban Phah",
            "mni" to "শন্দোক্নবা নম্বিয়ু",
            "nag" to "Chalu Kuribo Karone Dababi"
        ),

        // Tile 2: Brain Games
        "tile_games_title" to mapOf(
            "en" to "Brain Games",
            "hi" to "मस्तिष्क खेल",
            "as" to "মগজুৰ খেল",
            "lus" to "Rilru Infiamna",
            "kha" to "Ki Jingialehkai Jingmut",
            "mni" to "ৱাখল্লোন শান্নবা",
            "nag" to "Dimag laga Khel"
        ),
        "tile_games_sub" to mapOf(
            "en" to "2 Cognitive Modes",
            "hi" to "२ संज्ञानात्मक खेल",
            "as" to "২টা মগজুৰ অনুশীলন",
            "lus" to "Infiamna Chi 2",
            "kha" to "Ar Tylli Ki Jingialehkai",
            "mni" to "মখল অনি শান্নবা",
            "nag" to "2 Ta Khel Ase"
        ),

        // Tile 3: Cognitive Score
        "tile_score_title" to mapOf(
            "en" to "Cognitive Health",
            "hi" to "संज्ञानात्मक स्वास्थ्य",
            "as" to "মগজুৰ স্বাস্থ্য",
            "lus" to "Hriatna Hriselna",
            "kha" to "Ka Koit Ka Khiah Jingmut",
            "mni" to "ৱাখলগী হকশেল",
            "nag" to "Dimag laga Health"
        ),
        "tile_score_sub_untested" to mapOf(
            "en" to "Play Game to Assess",
            "hi" to "स्कोर हेतु खेल खेलें",
            "as" to "স্ক'ৰৰ বাবে খেল খেলক",
            "lus" to "Zir Chian Nan Infiam",
            "kha" to "Lehkai Ban Pynshai",
            "mni" to "স্কোরগীদমক শান্নবিয়ু",
            "nag" to "Score Karone Khelibi"
        ),
        "tile_score_sub_tested" to mapOf(
            "en" to "Assessment Ready",
            "hi" to "आकलन तैयार",
            "as" to "মূল্যায়ন প্ৰস্তুত",
            "lus" to "Endikna A Kim",
            "kha" to "La Dep Ka Jingbishar",
            "mni" to "য়েংশিনবা লোইরে",
            "nag" to "Score Ahi Jaise"
        ),

        // Tile 4: Safety Alerts
        "tile_safety_title" to mapOf(
            "en" to "Safety & Alerts",
            "hi" to "सुरक्षा एवं अलर्ट",
            "as" to "সুৰক্ষা আৰু সতৰ্কতা",
            "lus" to "Venhimna & Hriattirna",
            "kha" to "Ka Jingiada & Jingma",
            "mni" to "য়াম্না কনবা চেকশিনবা",
            "nag" to "Safety aru Alert"
        ),
        "tile_safety_sub" to mapOf(
            "en" to "Sundowning & Safe Zone",
            "hi" to "संध्या सिंड्रोम व सुरक्षित घेरा",
            "as" to "সন্ধ্যা সতর্কতা ও সুৰক্ষিত বলয়",
            "lus" to "Tlailam & Hmun Him",
            "kha" to "Jingpeit Syngam & Hmun Bha",
            "mni" to "নুমিদাং চেকশিনবা মফম",
            "nag" to "Shaam laga Alert aru Safe Zone"
        ),

        // Tile 5: Voice & Languages
        "tile_voice_title" to mapOf(
            "en" to "Languages & Voice",
            "hi" to "भाषाएं और आवाज",
            "as" to "ভাষা আৰু মাত",
            "lus" to "Tawng & Aw",
            "kha" to "Ki Ktien & Ka Jingkren",
            "mni" to "লোন অমসুং খোন্থোক",
            "nag" to "Bhasha aru Awaaz"
        ),
        "tile_voice_sub" to mapOf(
            "en" to "7 Regional Languages",
            "hi" to "७ क्षेत्रीय भाषाएं",
            "as" to "৭টা আঞ্চলিক ভাষা",
            "lus" to "Hnam Tawng 7",
            "kha" to "7 Tylli Ki Ktien",
            "mni" to "লমদমগী লোন ৭",
            "nag" to "7 Ta Local Bhasha"
        ),

        // Tile 6: Memory Vault
        "tile_memory_title" to mapOf(
            "en" to "Memory Vault",
            "hi" to "स्मृति संदूक",
            "as" to "স্মৃতি ভঁৰাল",
            "lus" to "Hriatrengna Hmun",
            "kha" to "Ka Synduk Jingkynmaw",
            "mni" to "নিংশিং মফম",
            "nag" to "Yaad laga Tijori"
        ),
        "tile_memory_sub" to mapOf(
            "en" to "Family Recall Therapy",
            "hi" to "पारिवारिक यादें",
            "as" to "পৰিয়ালৰ স্মৃতি সান্নিধ্য",
            "lus" to "Chhungkua Hriatletna",
            "kha" to "Jingkynmaw Iingsem",
            "mni" to "ইমুংগী নিংশিং থৌরম",
            "nag" to "Parivar laga Yaad"
        ),

        // Common Buttons & Actions
        "btn_back" to mapOf(
            "en" to "Back",
            "hi" to "वापस",
            "as" to "উভতি যাওক",
            "lus" to "Kir Leh",
            "kha" to "Leit Dien",
            "mni" to "হন্দোকপা",
            "nag" to "Pise Jabi"
        ),
        "btn_play_again" to mapOf(
            "en" to "Play Another Round",
            "hi" to "एक और राउंड खेलें",
            "as" to "আন এটি ৰাউণ্ড খেলক",
            "lus" to "Infiam Leh Rawh",
            "kha" to "Lehkai Biang Sa Chisien",
            "mni" to "অমুক হন্না শান্নবিয়ু",
            "nag" to "Aru Ek Bar Khelibi"
        ),
        "btn_start_test" to mapOf(
            "en" to "Play Brain Game to Assess",
            "hi" to "आकलन हेतु खेल शुरू करें",
            "as" to "মূল্যায়নৰ বাবে খেল আৰম্ভ কৰক",
            "lus" to "Infiamna Tan Rawh",
            "kha" to "Sdang Ia Ka Jingialehkai",
            "mni" to "শেন্নবগীদমক শান্নবা হৌবিয়ু",
            "nag" to "Khel Kheli Kene Test Kuri Lobi"
        ),
        "btn_listen_voice" to mapOf(
            "en" to "Hear Encouraging Voice",
            "hi" to "प्रोत्साहन ध्वनि सुनें",
            "as" to "উৎসাহজনক মাত শুনক",
            "lus" to "Tawngkam Phurna Ngaithla",
            "kha" to "Sngap Ia Ka Jingpynshai",
            "mni" to "থৌনা হাপ্পা খোন্থোক তাবিয়ু",
            "nag" to "Bhal Awaaz Huni Lobi"
        ),

        // Safety Bottom Pill
        "safety_bottom_pill" to mapOf(
            "en" to "Caregiver Safety & Safe Zone",
            "hi" to "देखभालकर्ता सुरक्षा व सुरक्षित घेरा",
            "as" to "যত্নলোৱাৰ সুৰক্ষা আৰু সুৰক্ষিত বলয়",
            "lus" to "Enkawltu Venhimna & Hmun Him",
            "kha" to "Ka Jingiada & Hmun Bha",
            "mni" to "য়েংশিনবগী চেকশিন মফম",
            "nag" to "Caregiver Safety aru Safe Zone"
        ),

        // Game Hub Strings
        "games_hub_title" to mapOf(
            "en" to "Cognitive Exercise Hub",
            "hi" to "संज्ञानात्मक व्यायाम केंद्र",
            "as" to "মগজুৰ অনুশীলন কেন্দ্ৰ",
            "lus" to "Rilru Zirna Hmun",
            "kha" to "Ka Hmun Pynshait Jingmut",
            "mni" to "ৱাখলগী কান্নবা শান্নফম",
            "nag" to "Dimag Kasrat Hub"
        ),
        "games_hub_sub" to mapOf(
            "en" to "Choose your daily cognitive journey",
            "hi" to "अपनी दैनिक दिमागी गतिविधि चुनें",
            "as" to "আপোনাৰ দৈনিক মগজুৰ খেল বাছনি কৰক",
            "lus" to "Vawiin i infiam duh thlang rawh",
            "kha" to "Jied ia ka jingialehkai jong phi",
            "mni" to "অদোমগী নোংমগী শান্নবা খনবিয়ু",
            "nag" to "Aji laga dimag khel basi lobi"
        ),
        "game1_name" to mapOf(
            "en" to "Memory Card Match",
            "hi" to "स्मृति कार्ड मिलान",
            "as" to "স্মৃতি কাৰ্ড মিলন",
            "lus" to "Thlalak Inmil Zawn",
            "kha" to "Pyniasnoh Ia Ki Kot",
            "mni" to "কার্দ নিংশিংবা অমসুং মান্নবা",
            "nag" to "Card Yaad Matching"
        ),
        "game1_desc" to mapOf(
            "en" to "Tap cards to uncover matching pairs",
            "hi" to "जोड़े खोजने के लिए कार्डों पर टैप करें",
            "as" to "যোৰা মিলাবলৈ কাৰ্ডবোৰ টিপক",
            "lus" to "A inmil zawng chhuak rawh",
            "kha" to "Kyntuit ban shem ia kiba iadei",
            "mni" to "মান্নবা ফংনবগীদমক কার্দশিং তাপীউ",
            "nag" to "Mili thaka juri paishe dababi"
        ),
        "game2_name" to mapOf(
            "en" to "Pattern & Sequence Logic",
            "hi" to "पैटर्न एवं अनुक्रम तर्क",
            "as" to "ক্ৰমিক প্ৰতিৰূপ যুক্তি",
            "lus" to "A Dawt Zawnna Hriatna",
            "kha" to "Jingithuh Ia Ka Jingiaid",
            "mni" to "মথং-মনাও নৈশিনবা ৱাখল",
            "nag" to "Pattern aru Sequence Logic"
        ),
        "game2_desc" to mapOf(
            "en" to "Memorize and repeat the glowing sequence",
            "hi" to "चमकते क्रम को याद रखें और दोहराएं",
            "as" to "জ্বলা ক্ৰমটো মনত ৰাখক আৰু পুনৰাবৃত্তি কৰক",
            "lus" to "Eng zuih zuih kha vawng reng la zawm rawh",
            "kha" to "Kynmaw ia ka jingthaba bad leh biang",
            "mni" to "ঙাল্লক্লিবা মথং অদু নিংশিংদুনা শান্নবিয়ু",
            "nag" to "Chamak thaka sequence yaad kuri kene dababi"
        ),
        "sequence_watch" to mapOf(
            "en" to "Watch closely...",
            "hi" to "ध्यान से देखें...",
            "as" to "মনোযোগেৰে চাওক...",
            "lus" to "Ngun takin en rawh...",
            "kha" to "Peit bha...",
            "mni" to "চেকশিন্না য়েংবিয়ু...",
            "nag" to "Dhyan te sabibi..."
        ),
        "sequence_your_turn" to mapOf(
            "en" to "Now repeat the pattern!",
            "hi" to "अब वही क्रम दोहराएं!",
            "as" to "এতিয়া অনুক্ৰমটো পুনৰাবৃত্তি কৰক!",
            "lus" to "I hun a thleng ve ta, zawm rawh!",
            "kha" to "Mynta ka dei ka pali jong phi!",
            "mni" to "হৌজিক অদোমগী খোঙথাংনি!",
            "nag" to "Etiya apuni laga bari, repeat koribi!"
        ),

        // Beacon Panel
        "beacon_title" to mapOf(
            "en" to "Live GPS Sentinel",
            "hi" to "लाइव जीपीएस प्रहरी",
            "as" to "লাইভ জি.পি.এছ. প্ৰহৰী",
            "lus" to "GPS Venhimna",
            "kha" to "Ka Jingithuh Shai GPS",
            "mni" to "লাইভ GPS য়েংশিনবা",
            "nag" to "Live GPS Sentinel"
        ),
        "beacon_subtitle" to mapOf(
            "en" to "Real-Time GPS & Caregiver Telemetry",
            "hi" to "रीयल-टाइम जीपीएस व देखभालकर्ता टेलीमेट्री",
            "as" to "প্ৰকৃত সময়ৰ জি.পি.এছ. আৰু যত্নলোৱাৰ তথ্য",
            "lus" to "GPS & Enkawltu Hriattirna",
            "kha" to "Jingtip Halor Ka Hmun & Nongsumar",
            "mni" to "লাইভ GPS অমসুং য়েংশিনবগী ঈ-পাউ",
            "nag" to "Real-Time GPS aru Caregiver Telemetry"
        ),
        "btn_turn_on_gps" to mapOf(
            "en" to "Turn ON Device GPS",
            "hi" to "डिवाइस जीपीएस चालू करें",
            "as" to "ডিভাইচৰ জি.পি.এছ. অন কৰক",
            "lus" to "GPS On Rawh",
            "kha" to "Plie Ia Ka GPS",
            "mni" to "GPS অন তৌবিয়ু",
            "nag" to "Device GPS On Koribi"
        ),
        "btn_grant_perms" to mapOf(
            "en" to "Grant Location Permissions",
            "hi" to "स्थान अनुमति प्रदान करें",
            "as" to "স্থানৰ অনুমতি প্ৰদান কৰক",
            "lus" to "Location Phallatna Pe Rawh",
            "kha" to "Ai Jingbit Ban Tip Ia Ka Hmun",
            "mni" to "মফম তাকপগী অয়াবা পীবিয়ু",
            "nag" to "Location Permission Dibi"
        ),
        "btn_start_broadcast" to mapOf(
            "en" to "Start Live Broadcasting",
            "hi" to "लाइव प्रसारण शुरू करें",
            "as" to "লাইভ সম্প্ৰচাৰ আৰম্ভ কৰক",
            "lus" to "Thawn Tan Rawh",
            "kha" to "Sdang Ban Phah",
            "mni" to "লাইভ শন্দোকপা হৌবিয়ু",
            "nag" to "Live Broadcasting Chalu Koribi"
        ),
        "btn_stop_broadcast" to mapOf(
            "en" to "Stop Broadcasting",
            "hi" to "प्रसारण रोकें",
            "as" to "সম্প্ৰচাৰ বন্ধ কৰক",
            "lus" to "Thawn Tihtawpna",
            "kha" to "Sangeh Ban Phah",
            "mni" to "শন্দোকপা লেপখ্রবু",
            "nag" to "Broadcasting Bondho Koribi"
        ),
        "lbl_address" to mapOf(
            "en" to "Physical Address",
            "hi" to "भौतिक पता",
            "as" to "ঠিকনা",
            "lus" to "Hmun Hming",
            "kha" to "Ka Haka Kaba Shisha",
            "mni" to "লৈফমগী ঠিকনা",
            "nag" to "Asol Thikana"
        ),
        "lbl_coordinates" to mapOf(
            "en" to "GPS Coordinates",
            "hi" to "जीपीएस निर्देशांक",
            "as" to "জি.পি.এছ. স্থানাংক",
            "lus" to "GPS Hmun Chhinna",
            "kha" to "Ki Dak Jingbuh GPS",
            "mni" to "GPS কোওর্ডিনেত",
            "nag" to "GPS Coordinates"
        ),
        "lbl_battery" to mapOf(
            "en" to "Battery Level",
            "hi" to "बैटरी स्तर",
            "as" to "বেটাৰীৰ মাত্ৰা",
            "lus" to "Battery San Zawng",
            "kha" to "Ka Bor Ka Battery",
            "mni" to "বেত্তরীগী চাং",
            "nag" to "Battery Level"
        ),
        "lbl_speed" to mapOf(
            "en" to "Movement Speed",
            "hi" to "गति की रफ़्तार",
            "as" to "গতিৰ বেগ",
            "lus" to "Kal Chak Zawng",
            "kha" to "Ka Jingstet Ka Jingiaid",
            "mni" to "খোঙজেলগী য়াম্বা",
            "nag" to "Speed"
        ),
        "lbl_safe_zone" to mapOf(
            "en" to "Safe Zone Status",
            "hi" to "सुरक्षित क्षेत्र स्थिति",
            "as" to "সুৰক্ষিত মণ্ডলৰ স্থিতি",
            "lus" to "Hmun Him Dinhmun",
            "kha" to "Ka Hmun Bha",
            "mni" to "চেকশিন মফমগী ফীভম",
            "nag" to "Safe Zone laga Halat"
        ),
        "status_inside_safe_zone" to mapOf(
            "en" to "Inside Safe Zone",
            "hi" to "सुरक्षित घेरे के अंदर",
            "as" to "সুৰক্ষিত বলয়ৰ ভিতৰত",
            "lus" to "Hmun Him Chhungah",
            "kha" to "Hapoh Ka Hmun Ba Bha",
            "mni" to "চেকশিন মফম মনুংদা",
            "nag" to "Safe Zone te Ase"
        ),
        "status_breach" to mapOf(
            "en" to "🚨 OUTSIDE SAFE ZONE",
            "hi" to "🚨 सुरक्षित घेरे से बाहर",
            "as" to "🚨 সুৰক্ষিত বলয়ৰ বাহিৰত",
            "lus" to "🚨 HMUN HIM PAWN",
            "kha" to "🚨 SHA BAR KA HMUN",
            "mni" to "🚨 চেকশিন মফম মপানদা",
            "nag" to "🚨 SAFE ZONE BAHAR"
        ),

        // Safety Panel
        "safety_title" to mapOf(
            "en" to "Safety & Emergency",
            "hi" to "सुरक्षा एवं आपातकालीन",
            "as" to "সুৰক্ষা আৰু জৰুৰীকালীন",
            "lus" to "Venhimna & Chhanhimna",
            "kha" to "Ka Jingiada & Jingeh",
            "mni" to "য়াম্না কনবা চেকশিনবা",
            "nag" to "Safety aru Emergency"
        ),
        "safety_subtitle" to mapOf(
            "en" to "Emergency SOS, Home Directions & Wander Alerts",
            "hi" to "आपातकालीन एसओएस, घर का रास्ता व भटकाव अलर्ट",
            "as" to "জৰুৰীকালীন SOS, ঘৰলৈ দিশ আৰু পথভ্ৰষ্ট সতৰ্কতা",
            "lus" to "SOS, In Panna Kawng & Hriattirna",
            "kha" to "SOS, Ka Lynti Sha Iing & Jingpeit",
            "mni" to "SOS, য়ুমগী লম্বী অমসুং চেকশিনবা",
            "nag" to "Emergency SOS, Ghor laga Rasta aru Alert"
        ),
        "btn_take_me_home" to mapOf(
            "en" to "TAKE ME HOME (Directions)",
            "hi" to "मुझे घर ले चलो (रास्ता)",
            "as" to "মোক ঘৰলৈ লৈ যাওক (দিশ)",
            "lus" to "IN AH MIN HRUAIRAW (Kawng)",
            "kha" to "IALAM SHA IING (Lynti)",
            "mni" to "য়ুমদা পুখ্রবু (লম্বী)",
            "nag" to "MOKE GHOR LOI JABI (Rasta)"
        ),
        "btn_set_home" to mapOf(
            "en" to "Set Home Safe Zone",
            "hi" to "घर का सुरक्षित स्थान सेट करें",
            "as" to "ঘৰৰ সুৰক্ষিত স্থান নিৰ্ধাৰণ কৰক",
            "lus" to "In Hmun Him Siambawl",
            "kha" to "Buh Ia Ka Iing Bha",
            "mni" to "য়ুমগী চেকশিন মফম সেমগৎলু",
            "nag" to "Ghor laga Safe Zone Set Koribi"
        ),
        "btn_use_current_gps" to mapOf(
            "en" to "Use Current GPS Location as Home",
            "hi" to "वर्तमान जीपीएस स्थान को घर के रूप में उपयोग करें",
            "as" to "বৰ্তমানৰ জি.পি.এছ. অৱস্থান ঘৰ হিচাপে ব্যৱহাৰ কৰক",
            "lus" to "Tunlai GPS hmang hian in siam rawh",
            "kha" to "Pyndonkam ia ka GPS mynta kum ka iing",
            "mni" to "হৌজিক্কী GPS অসি য়ুম ওইনা লৌবিয়ু",
            "nag" to "Etiya laga GPS jagah ghor hisabte lobo"
        ),
        "btn_save_home" to mapOf(
            "en" to "Save Home Location",
            "hi" to "घर का स्थान सहेजें",
            "as" to "ঘৰৰ অৱস্থান সংৰক্ষণ কৰক",
            "lus" to "In Hmun Vawng Tha Rawh",
            "kha" to "Kynshew Ia Ka Hmun Iing",
            "mni" to "য়ুমগী মফম সংৰক্ষণ তৌবিয়ু",
            "nag" to "Ghor laga Jagah Save Koribi"
        ),

        // Memory Vault Panel
        "memory_title" to mapOf(
            "en" to "Memory Vault",
            "hi" to "स्मृति संदूक",
            "as" to "স্মৃতি ভঁৰাল",
            "lus" to "Hriatrengna Hmun",
            "kha" to "Ka Synduk Jingkynmaw",
            "mni" to "নিংশিং মফম",
            "nag" to "Yaad laga Tijori"
        ),
        "memory_subtitle" to mapOf(
            "en" to "Autobiographical Family Recall",
            "hi" to "पारिवारिक यादें और संस्मरण",
            "as" to "পৰিয়ালৰ স্মৃতি সান্নিধ্য",
            "lus" to "Chhungkua Hriatletna",
            "kha" to "Jingkynmaw Iingsem",
            "mni" to "ইমুংগী নিংশিং থৌরম",
            "nag" to "Parivar laga Yaad"
        ),
        "btn_add_memory" to mapOf(
            "en" to "Add Memory",
            "hi" to "याद जोड़ें",
            "as" to "স্মৃতি যোগ কৰক",
            "lus" to "Hriatna Belh",
            "kha" to "Buh Jingkynmaw",
            "mni" to "নিংশিংবা হাপচিল্লু",
            "nag" to "Yaad Milabi"
        ),
        "lbl_no_memories" to mapOf(
            "en" to "No Memory Cards Yet",
            "hi" to "अभी कोई स्मृति कार्ड नहीं है",
            "as" to "এতিয়ালৈকে কোনো স্মৃতি কাৰ্ড নাই",
            "lus" to "Hriatrengna Card A La Awm Lo",
            "kha" to "Ym Pat Don Kot Jingkynmaw",
            "mni" to "নিংশিং কার্দ অমত্তা লৈত্রি",
            "nag" to "Kiba Yaad Card Nai Etiya"
        ),
        "lbl_no_memories_desc" to mapOf(
            "en" to "Family members can add custom recall questions and moments above!",
            "hi" to "परिवार के सदस्य ऊपर अपनी तस्वीरें और संस्मरण प्रश्न जोड़ सकते हैं!",
            "as" to "পৰিয়ালৰ সদস্যসকলে ওপৰত স্মৃতিমূলক প্ৰশ্ন যোগ কৰিব পাৰে!",
            "lus" to "Chhungte hian zawhna leh thlalak an dah lut thei e!",
            "kha" to "Ki bahaing ki lah ban buh ki jingkylli kynmaw!",
            "mni" to "ইমুংগী মীশিংনা য়াথং অদু হাপচিনবা য়াগনি!",
            "nag" to "Ghor laga manu khan upar te photo aru prashna milabo pare!"
        ),

        // Health Score Panel
        "health_title" to mapOf(
            "en" to "Cognitive Health Score",
            "hi" to "संज्ञानात्मक स्वास्थ्य स्कोर",
            "as" to "মগজুৰ স্বাস্থ্য মূল্যায়ন",
            "lus" to "Hriatna Hriselna Score",
            "kha" to "Ka Jingkhein Koit Khiah Jingmut",
            "mni" to "ৱাখলগী হকশেল স্কোর",
            "nag" to "Dimag Health Score"
        ),
        "health_subtitle" to mapOf(
            "en" to "Clinical Telemetry Analysis (SIH26003)",
            "hi" to "क्लिनिकल टेलीमेट्री विश्लेषण",
            "as" to "চিকিৎসাভিত্তিক তথ্য বিশ্লেষণ",
            "lus" to "Enkawlna Lam Endikna",
            "kha" to "Jingbishar Koit Khiah",
            "mni" to "হকশেলগী নৈশিনবা",
            "nag" to "Clinical Telemetry Analysis"
        ),
        "lbl_untested" to mapOf(
            "en" to "No Assessment Recorded Yet Today",
            "hi" to "आज अभी तक कोई आकलन दर्ज नहीं है",
            "as" to "আজি এতিয়ালৈকে কোনো মূল্যায়ন হোৱা নাই",
            "lus" to "Vawiin Endikna A La Awm Lo",
            "kha" to "Ym Pat Don Jingkhein Mynta",
            "mni" to "ঙসি অমত্তা য়েংশিনদ্রি",
            "nag" to "Aji kiba test kora nai"
        ),
        "lbl_untested_desc" to mapOf(
            "en" to "Complete any daily cognitive exercise to compute your genuine clinical CPS score.",
            "hi" to "अपना वास्तविक नैदानिक सीपीएस स्कोर जानने के लिए कोई भी दैनिक खेल खेलें।",
            "as" to "আপোনাৰ প্ৰকৃত ক্লিনিকল চি.পি.এছ. স্ক'ৰ গণনা কৰিবলৈ যিকোনো খেল সম্পূৰ্ণ কৰক।",
            "lus" to "CPS score hre turin infiamna vawi khat tal khel rawh.",
            "kha" to "Lehkai ban ioh ia ka CPS score.",
            "mni" to "CPS স্কোর ফংনবগীদমক অদোম শান্নবিয়ু।",
            "nag" to "Apuni laga CPS score pabo karone kiba ekta khel khelibi."
        ),

        // Games Hub Panel
        "game3_title" to mapOf(
            "en" to "Color-Word Stroop Focus",
            "hi" to "रंग-शब्द स्ट्रोप एकाग्रता",
            "as" to "ৰং-শব্দ ষ্ট্ৰুপ মনোযোগ",
            "lus" to "Rawng & Thu Inmil Zawn",
            "kha" to "Ka Rong Bad Ktien Jingpyrkhat",
            "mni" to "মচু অমসুং ৱাহৈ নৈশিনবা",
            "nag" to "Rang aru Kotha Focus"
        ),
        "game3_desc" to mapOf(
            "en" to "10 fast-paced rounds evaluating executive cognitive inhibition",
            "hi" to "१० तीव्र राउंड मानसिक अवरोध व एकाग्रता हेतु",
            "as" to "মনোযোগ বৃদ্ধিৰ বাবে ১০টা তীব্ৰ ৰাউণ্ড",
            "lus" to "Rilru sawizawi nan vawi 10 khelh tur",
            "kha" to "10 tylli ki jingialehkai ban pynkhlain jingmut",
            "mni" to "ৱাখলগী কান্নবা রাউন্দ ১০",
            "nag" to "10 ta round dimag focus karone"
        ),
        "game4_title" to mapOf(
            "en" to "Ascending Number Trail",
            "hi" to "आरोही संख्या पथ",
            "as" to "উৰ্ধ্বমুখী সংখ্যাৰ ক্ৰম",
            "lus" to "Number Inzawm Zawn",
            "kha" to "Ki Dak Jingkhein Kiew",
            "mni" to "মশীংগী মথং-মনাও লম্বী",
            "nag" to "Number Trail Khel"
        ),
        "game4_desc" to mapOf(
            "en" to "Multi-round sequence trail evaluating visual scanning & motor speed",
            "hi" to "दृष्टि व गतिशीलता हेतु बहु-राउंड संख्या क्रम",
            "as" to "দৃষ্টি আৰু মানসিক গতিৰ বাবে সংখ্যা ক্ৰম",
            "lus" to "Mit leh kut inmil zawnna",
            "kha" to "Pyniasnoh ia ki dak jingkhein",
            "mni" to "মিৎ অমসুং খুৎকী চৎনবা",
            "nag" to "Number scan aru speed khel"
        ),
        "lbl_reminder_interval" to mapOf(
            "en" to "Game Reminder Interval",
            "hi" to "खेल अनुस्मारक अंतराल",
            "as" to "খেলৰ সোঁৱৰণিৰ ব্যৱধান",
            "lus" to "Infiamna Hriattirna Hun",
            "kha" to "Ka Por Pynkynmaw Lehkai",
            "mni" to "শান্নবগী নিংশিংবা মতম",
            "nag" to "Game Reminder Time"
        ),
        "lbl_reminder_interval_desc" to mapOf(
            "en" to "Plays loud alarm sound and displays popping colors even when app is closed",
            "hi" to "ऐप बंद होने पर भी तेज़ अलार्म बजेगा और रंग दिखाई देंगे",
            "as" to "এপ বন্ধ থাকিলেও উচ্চ শব্দত এলার্ম বাজিব আৰু ৰং জিলিকিব",
            "lus" to "App khar mahse ri ring tak leh rawng mawi tak a lo lang ang",
            "kha" to "Wat la khang ia ka app, kan sawa bad pyni rong",
            "mni" to "এপ থিংজিল্লবসু খুন্থোক কেন্না তারগনি",
            "nag" to "App bondho thakile bhi awaz aru rong ulai jabo"
        ),
        "btn_test_alarm" to mapOf(
            "en" to "Test Loud Alarm Now (3s)",
            "hi" to "अभी तेज़ अलार्म का परीक्षण करें (3s)",
            "as" to "এতিয়াই উচ্চ এলার্ম পৰীক্ষা কৰক (৩ ছেকেণ্ড)",
            "lus" to "Alarm Chhin Chhinna (3s)",
            "kha" to "Pyrshang Ia Ka Alarm Mynta (3s)",
            "mni" to "এলার্ম চাংয়েং তৌবিয়ু (৩s)",
            "nag" to "Etiya Loud Alarm Test Koribi (3s)"
        ),
        "lbl_round" to mapOf(
            "en" to "Round",
            "hi" to "राउंड",
            "as" to "ৰাউণ্ড",
            "lus" to "Round",
            "kha" to "Kyntien",
            "mni" to "রাউন্দ",
            "nag" to "Round"
        ),
        "lbl_of" to mapOf(
            "en" to "of",
            "hi" to "का",
            "as" to "/",
            "lus" to "/",
            "kha" to "na",
            "mni" to "/",
            "nag" to "porate"
        ),
        "lbl_score" to mapOf(
            "en" to "Score",
            "hi" to "अंक",
            "as" to "স্ক'ৰ",
            "lus" to "Score",
            "kha" to "Jingioh",
            "mni" to "স্কোর",
            "nag" to "Score"
        ),
        "lbl_level_cleared" to mapOf(
            "en" to "Level Cleared! Next level...",
            "hi" to "स्तर पूरा हुआ! अगला स्तर...",
            "as" to "স্তৰ সম্পূৰ্ণ! পৰৱৰ্তী স্তৰ...",
            "lus" to "I zo ta! A dawt leh...",
            "kha" to "La dep! Kawei pat...",
            "mni" to "লেভেল লোইরে! মথংগী...",
            "nag" to "Level Pass Hoise! Aru aage..."
        ),
        "lbl_level" to mapOf(
            "en" to "Level",
            "hi" to "स्तर",
            "as" to "স্তৰ",
            "lus" to "Zirna",
            "kha" to "Kyrdan",
            "mni" to "লেভেল",
            "nag" to "Level"
        ),
        "sub_memory" to mapOf(
            "en" to "Memory Retention",
            "hi" to "स्मृति अवधारण",
            "as" to "স্মৃতি ধাৰণ",
            "lus" to "Hriatrengna Vawn Nun",
            "kha" to "Kynmaw Bha",
            "mni" to "নীংশিংবা থম্বা",
            "nag" to "Yaad Rakhibo Pora"
        ),
        "sub_executive" to mapOf(
            "en" to "Executive Function",
            "hi" to "कार्यकारी क्षमता",
            "as" to "কাৰ্যনিৰ্বাহী ক্ষমতা",
            "lus" to "Thluak Hman Thiamna",
            "kha" to "Borabor Jingtrei Kam",
            "mni" to "থবক তৌবগী ৱাখল",
            "nag" to "Kaam Dimag Power"
        ),
        "sub_reaction" to mapOf(
            "en" to "Reaction Latency",
            "hi" to "प्रतिक्रिया समय",
            "as" to "প্ৰতিক্ৰিয়াৰ সময়",
            "lus" to "Chhan Let Zung Zungna",
            "kha" to "Ka Por Ban Jubab",
            "mni" to "থোক্লকপা মতম",
            "nag" to "Reaction Time"
        ),
        "sub_autobio" to mapOf(
            "en" to "Autobiographical Recall",
            "hi" to "व्यक्तिगत स्मृति स्मरण",
            "as" to "আত্মজীৱনীমূলক স্মৃতি",
            "lus" to "Mahni Chanchin Hriatletna",
            "kha" to "Kynmaw Ia La Ka Jingim",
            "mni" to "মশাগী পুন্সি নীংশিংবা",
            "nag" to "Nijor Yaad Ahibo"
        ),
        "sub_recovery" to mapOf(
            "en" to "Error Recovery",
            "hi" to "त्रुटि सुधार दर",
            "as" to "ভুল সংশোধন ক্ষমতা",
            "lus" to "Siamṭhat Leh Varna",
            "kha" to "Pynbha Biang Ia Ka Jingbakla",
            "mni" to "সোইবা শেমদোকপা",
            "nag" to "Galti Thik Kora Rate"
        ),
        "game1_hint" to mapOf(
            "en" to "Tap cards to uncover matching pairs",
            "hi" to "जोड़े खोजने के लिए कार्डों पर टैप करें",
            "as" to "যোৰা মিলাবলৈ কাৰ্ডবোৰ টিপক",
            "lus" to "A inmil zawng chhuak rawh",
            "kha" to "Pyniasnoh ia kiba iadei",
            "mni" to "মান্নবা কার্দশিং খনবিয়ু",
            "nag" to "Card juri milabi"
        ),
        "game3_hint" to mapOf(
            "en" to "Tap the INK COLOR",
            "hi" to "स्याही का रंग चुनें",
            "as" to "চিয়াহীৰ ৰংটো বাছক",
            "lus" to "A rawng dik thlang rawh",
            "kha" to "Jied ia ka rong shisha",
            "mni" to "মচু অদু খনবিয়ু",
            "nag" to "Rang basi lobi"
        ),
        "game4_hint" to mapOf(
            "en" to "Tap numbers in ascending order",
            "hi" to "बढ़ते क्रम में संख्याओं पर टैप करें",
            "as" to "সংখ্যাবোৰ ক্ৰমানুসাৰে টিপক",
            "lus" to "Number inzawm indawtin hmet rawh",
            "kha" to "Kyntuit ia ki dak jingkhein",
            "mni" to "মশীং মথং-মনাও নম্বিয়ু",
            "nag" to "Number ekta ekta dababi"
        ),

        // Full Screen Alarm Screen
        "alarm_title" to mapOf(
            "en" to "BRAIN EXERCISE TIME",
            "hi" to "मस्तिष्क व्यायाम का समय",
            "as" to "মগজুৰ অনুশীলনৰ সময়",
            "lus" to "RILRU INFIAHNA HUN",
            "kha" to "KA POR LEHKAI JINGMUT",
            "mni" to "ৱাখলগী এক্সরসাইজগী মতম",
            "nag" to "DIMAG KASRAT LAGA TIME"
        ),
        "alarm_desc" to mapOf(
            "en" to "Keep your mind sharp! It's time for your scheduled memory and focus exercises.",
            "hi" to "अपने दिमाग को तेज़ रखें! यह आपके निर्धारित स्मृति और ध्यान अभ्यास का समय है।",
            "as" to "আপোনাৰ মন সজীৱ ৰাখক! স্মৃতি আৰু মনোযোগ বৃদ্ধিৰ খেল খেলক।",
            "lus" to "I rilru tiharh rawh! Hriatna leh rilru sawizawi hun a thleng ta.",
            "kha" to "Pynshait ia ka jingmut! Ka dei ka por ban pynkhlain ia ka jingkynmaw.",
            "mni" to "অদোমগী ৱাখল শেমগৎলু! নিংশিং অমসুং পুক্নিং চংবগী মতম ওইরে।",
            "nag" to "Dimag tez rakhabi! Apuni laga yaad aru dhyan khel laga time hoise."
        ),
        "alarm_btn_play" to mapOf(
            "en" to "START BRAIN GAME NOW",
            "hi" to "दिमागी खेल शुरू करें",
            "as" to "মগজুৰ খেল আৰম্ভ কৰক",
            "lus" to "INFIAMNA TAN RAWH",
            "kha" to "SDANG LEHKAI MYNTA",
            "mni" to "হৌজিক শান্নবা হৌবিয়ু",
            "nag" to "ETIYA DIMAG KHEL CHALU KORIBI"
        ),
        "alarm_btn_snooze" to mapOf(
            "en" to "Snooze for 10 Minutes",
            "hi" to "१० मिनट बाद याद दिलाएं",
            "as" to "১০ মিনিটৰ পিছত সোঁৱৰাব",
            "lus" to "Minute 10 hnuah hriattir leh rawh",
            "kha" to "10 minit pynsangeh",
            "mni" to "মিনিত ১০ তুংদা নিংশিংবিয়ু",
            "nag" to "10 minute pise yaad dibi"
        )
    )

    private val encouragements = mapOf(
        "gentle" to mapOf(
            "en" to "Wonderful effort! You are doing great. Let's enjoy another fun memory activity!",
            "hi" to "बहुत सुंदर प्रयास! आप बहुत अच्छा खेल रहे हैं। चलिए अगला मजेदार स्मृति खेल खेलते हैं!",
            "as" to "অতি সুন্দৰ! আপুনি বহুত ভাল খেলিছে। বলক আন এটি ধুনীয়া স্মৃতি খেল খেলো।",
            "lus" to "Thawk tha tak tling i ni! A nuam dang i zir zel ang u.",
            "kha" to "Ka jingseimot kaba bha shibun! To ngin ia iaid shakhmat paralok.",
            "mni" to "য়াম্না ফবা হোৎনবনি! অদোম য়াম্না ফনা শান্নরি। মথংগী হরাওবা নিংশিং শান্নবা শান্নসি!",
            "nag" to "Bhal kosish ase! Apuni bhal kheli ase. Aru ekta bhal dimag kheli khelibo ahibi!"
        ),
        "encouraging" to mapOf(
            "en" to "Fantastic progress! Your focus is super sharp today. Let's keep exploring!",
            "hi" to "शानदार प्रगति! आपका ध्यान आज बहुत तेज़ है। चलिए आगे बढ़ते हैं!",
            "as" to "চমৎকার উন্নতি! আপোনাৰ মনোযোগ সঁচাকৈয়ে প্রশংসনীয়।",
            "lus" to "I puitlinna a tha hle mai! I rilru a fim tha hle.",
            "kha" to "Ka jingkiew kaba khraw! Ka jingmut jong phi ka long kaba shai halor kiei kiei.",
            "mni" to "চাউখৎপা খোঙথাংনি! ঙসিদি অদোমগী পুক্নিং য়াম্না থৌনা লৈ। মাংলোয়ননা চত্থসি!",
            "nag" to "Bishi bhal aguwai ase! Apuni laga dhyan aji bishi bhal ase. Aru aage jabi!"
        ),
        "celebratory" to mapOf(
            "en" to "Outperforming excellence! You are a master memory explorer today!",
            "hi" to "असाधारण प्रतिभा! आज आप वाकई एक महान स्मृति विजेता हैं!",
            "as" to "অসাধাৰণ দক্ষতা! আপুনি আজি সঁচাকৈয়ে এজন মহান স্মৃতি বিজয়ী!",
            "lus" to "A tha tawpkhawk hle mai! Vawiin chu i thluak a chak zual hle.",
            "kha" to "Ka jingshai kaba khraw tarn! Phi long u nongjop uba bakhraw ha ka jingkynmaw.",
            "mni" to "থোইদোক-হেন্দোকপা হৈশিংবনি! অদোম ঙসি নিংশিংবগী অচেৎপা মাইপাকপা অমনি!",
            "nag" to "Ekdum zabardast! Aji toh apuni asol dimag laga champion hoise!"
        )
    )

    private val voiceConfirmations = mapOf(
        "en" to "Voice guidance set to English.",
        "hi" to "ध्वनि मार्गदर्शन हिंदी में सेट किया गया है।",
        "as" to "অসমীয়া ভাষাত মাতৰ নিৰ্দেশনা সক্ৰিয় কৰা হৈছে।",
        "lus" to "Aw hmanga kaihhruaina hi Mizo tawngin siam a ni.",
        "kha" to "Jingpynshai ha ka jien Khasi la pynkyntu.",
        "mni" to "মৈতৈলোন্দা খোন্থোক্কী ৱাফম শেম্লে।",
        "nag" to "Awaaz guide toh Nagamese te set kurishey."
    )

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    fun initTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.ENGLISH
            }
        }
    }

    fun speak(text: String, langCode: String = "en") {
        if (!isTtsReady || tts == null) return
        try {
            when (langCode) {
                "hi" -> tts?.language = Locale("hi", "IN")
                "as", "lus", "kha", "mni", "nag" -> {
                    val inLocale = Locale("en", "IN")
                    tts?.language = inLocale
                }
                else -> tts?.language = Locale.ENGLISH
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "encouragement_utterance")
        } catch (_: Exception) {}
    }

    /**
     * Translates any string key dynamically to the selected language.
     * Falls back to English if missing in target dialect.
     */
    fun tr(key: String, langCode: String): String {
        val entry = stringRepository[key] ?: return key
        return entry[langCode] ?: entry["en"] ?: key
    }

    fun getEncouragement(level: String, langCode: String): String {
        val tier = encouragements[level] ?: encouragements["encouraging"]!!
        return tier[langCode] ?: tier["en"] ?: "Wonderful effort! You are doing great!"
    }

    fun getVoiceConfirmation(langCode: String): String {
        return voiceConfirmations[langCode] ?: voiceConfirmations["en"]!!
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (_: Exception) {}
    }
}
