package com.example.data.local

import android.content.Context
import com.example.ui.screens.Surah
import com.example.ui.screens.Verse
import org.json.JSONArray
import java.io.InputStream
import java.nio.charset.StandardCharsets

object QuranData {

    fun getSurahsList(context: Context): List<Surah> {
        return loadSurahsFromAssets(context)
    }

    private fun loadSurahsFromAssets(context: Context): List<Surah> {
        val metadata = getSurahMetadata()
        val versesMap = mutableMapOf<Int, MutableList<Verse>>()
        
        try {
            val jsonString = context.assets.open("quran.json").bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val surahNumStr = keys.next()
                val surahNum = surahNumStr.toIntOrNull() ?: continue
                val versesArray = jsonObject.getJSONArray(surahNumStr)
                
                val verses = mutableListOf<Verse>()
                for (i in 0 until versesArray.length()) {
                    val verseObj = versesArray.getJSONObject(i)
                    val verseNum = verseObj.optInt("verse", i + 1)
                    val arabic = verseObj.optString("text", "")
                    
                    verses.add(Verse(number = verseNum, arabic = arabic, translation = ""))
                }
                versesMap[surahNum] = verses
            }
        } catch (t: Throwable) {
            android.util.Log.e("QuranData", "Error parsing quran.json", t)
        }

        return metadata.map { meta ->
            val verses = versesMap[meta.number] ?: emptyList()
            // Ensure verses are sorted in correct ascending order of verse number
            val sortedVerses = verses.sortedBy { it.number }
            
            Surah(
                number = meta.number,
                nameEnglish = meta.nameEnglish,
                nameArabic = meta.nameArabic,
                translation = meta.translation,
                versesCount = if (sortedVerses.isNotEmpty()) sortedVerses.size else meta.versesCount,
                type = meta.type,
                verses = sortedVerses.ifEmpty {
                    // Fail-safe default so we never crash the application if asset loading has issues
                    (1..meta.versesCount).map { vNum ->
                        Verse(
                            number = vNum,
                            arabic = "آية ${vNum}",
                            translation = "Verse ${vNum} of ${meta.nameEnglish}"
                        )
                    }
                }
            )
        }
    }

    private fun getSurahMetadata(): List<SurahMeta> {
        return listOf(
            SurahMeta(1, "Al-Fatihah", "الفاتحة", "The Opening", 7, "Meccan"),
            SurahMeta(2, "Al-Baqarah", "البقرة", "The Cow", 286, "Medinan"),
            SurahMeta(3, "Ali 'Imran", "آل عمران", "Family of Imran", 200, "Medinan"),
            SurahMeta(4, "An-Nisa", "النساء", "The Women", 176, "Medinan"),
            SurahMeta(5, "Al-Ma'idah", "المائدة", "The Table Spread", 120, "Medinan"),
            SurahMeta(6, "Al-An'am", "الأنعام", "The Cattle", 165, "Meccan"),
            SurahMeta(7, "Al-A'raf", "الأعراف", "The Heights", 206, "Meccan"),
            SurahMeta(8, "Al-Anfal", "الأنفال", "The Spoils of War", 75, "Medinan"),
            SurahMeta(9, "At-Tawbah", "التوبة", "The Repentance", 129, "Medinan"),
            SurahMeta(10, "Yunus", "يونس", "Jonah", 109, "Meccan"),
            SurahMeta(11, "Hud", "هود", "Hud", 123, "Meccan"),
            SurahMeta(12, "Yusuf", "يوسف", "Joseph", 111, "Meccan"),
            SurahMeta(13, "Ar-Ra'd", "الرعد", "The Thunder", 43, "Medinan"),
            SurahMeta(14, "Ibrahim", "إبراهيم", "Abraham", 52, "Meccan"),
            SurahMeta(15, "Al-Hijr", "الحجر", "The Rocky Tract", 99, "Meccan"),
            SurahMeta(16, "An-Nahl", "النحل", "The Bee", 128, "Meccan"),
            SurahMeta(17, "Al-Isra", "الإسراء", "The Night Journey", 111, "Meccan"),
            SurahMeta(18, "Al-Kahf", "الكهف", "The Cave", 110, "Meccan"),
            SurahMeta(19, "Maryam", "مريم", "Mary", 98, "Meccan"),
            SurahMeta(20, "Taha", "طه", "Ta-Ha", 135, "Meccan"),
            SurahMeta(21, "Al-Anbiya", "الأنبياء", "The Prophets", 112, "Meccan"),
            SurahMeta(22, "Al-Hajj", "الحج", "The Pilgrimage", 78, "Medinan"),
            SurahMeta(23, "Al-Mu'minun", "المؤمنون", "The Believers", 118, "Meccan"),
            SurahMeta(24, "An-Nur", "النور", "The Light", 64, "Medinan"),
            SurahMeta(25, "Al-Furqan", "الفرقمان", "The Criterion", 77, "Meccan"),
            SurahMeta(26, "Ash-Shu'ara", "الشعراء", "The Poets", 227, "Meccan"),
            SurahMeta(27, "An-Naml", "النمل", "The Ant", 93, "Meccan"),
            SurahMeta(28, "Al-Qasas", "القصص", "The Stories", 88, "Meccan"),
            SurahMeta(29, "Al-Ankabut", "العنكبوت", "The Spider", 69, "Meccan"),
            SurahMeta(30, "Ar-Rum", "الروم", "The Romans", 60, "Meccan"),
            SurahMeta(31, "Luqman", "لقمان", "Luqman", 34, "Meccan"),
            SurahMeta(32, "As-Sajdah", "السجدة", "The Prostration", 30, "Meccan"),
            SurahMeta(33, "Al-Ahzab", "الأحزاب", "The Combined Forces", 73, "Medinan"),
            SurahMeta(34, "Saba", "سبأ", "Sheba", 54, "Meccan"),
            SurahMeta(35, "Fatir", "فاطر", "Originator", 45, "Meccan"),
            SurahMeta(36, "Ya-Sin", "يس", "Ya-Sin", 83, "Meccan"),
            SurahMeta(37, "As-Saffat", "الصافات", "Those who set the Ranks", 182, "Meccan"),
            SurahMeta(38, "Sad", "ص", "Sad", 88, "Meccan"),
            SurahMeta(39, "Az-Zumar", "الزمر", "The Troops", 75, "Meccan"),
            SurahMeta(40, "Ghafir", "غافر", "The Forgiver", 85, "Meccan"),
            SurahMeta(41, "Fussilat", "فصلت", "Explained in Detail", 54, "Meccan"),
            SurahMeta(42, "Ash-Shura", "الشورى", "The Consultation", 53, "Meccan"),
            SurahMeta(43, "Az-Zukhruf", "الزخرف", "The Ornaments of Gold", 89, "Meccan"),
            SurahMeta(44, "Ad-Dukhan", "الدخان", "The Smoke", 59, "Meccan"),
            SurahMeta(45, "Al-Jathiyah", "الجاثية", "The Crouching", 37, "Meccan"),
            SurahMeta(46, "Al-Ahqaf", "الأحقاف", "The Sandhills", 35, "Meccan"),
            SurahMeta(47, "Muhammad", "محمد", "Muhammad", 38, "Medinan"),
            SurahMeta(48, "Al-Fath", "الفتح", "The Victory", 29, "Medinan"),
            SurahMeta(49, "Al-Hujurat", "الحجرات", "The Dwellings", 18, "Medinan"),
            SurahMeta(50, "Qaf", "ق", "Qaf", 45, "Meccan"),
            SurahMeta(51, "Adh-Dhariyat", "الذاريات", "The Winnowing Winds", 60, "Meccan"),
            SurahMeta(52, "At-Tur", "الطور", "The Mount", 49, "Meccan"),
            SurahMeta(53, "An-Najm", "النجم", "The Star", 62, "Meccan"),
            SurahMeta(54, "Al-Qamar", "القمر", "The Moon", 55, "Meccan"),
            SurahMeta(55, "Ar-Rahman", "الرحمن", "The Beneficent", 78, "Medinan"),
            SurahMeta(56, "Al-Waqi'ah", "الواقعة", "The Inevitable", 96, "Meccan"),
            SurahMeta(57, "Al-Hadid", "الحديد", "The Iron", 29, "Medinan"),
            SurahMeta(58, "Al-Mujadilah", "المجادلة", "The Pleading Woman", 22, "Medinan"),
            SurahMeta(59, "Al-Hashr", "الحشر", "The Exile", 24, "Medinan"),
            SurahMeta(60, "Al-Mumtahanah", "الممتحنة", "She That is to be Examined", 13, "Medinan"),
            SurahMeta(61, "As-Saff", "الصف", "The Ranks", 14, "Medinan"),
            SurahMeta(62, "Al-Jumu'ah", "الجمعة", "The Congregation", 11, "Medinan"),
            SurahMeta(63, "Al-Munafiqun", "المنافقون", "The Hypocrites", 11, "Medinan"),
            SurahMeta(64, "At-Taghabun", "التغابن", "The Mutual Disillusion", 18, "Medinan"),
            SurahMeta(65, "At-Talaq", "الطلاق", "The Divorce", 12, "Medinan"),
            SurahMeta(66, "At-Tahrim", "التحريم", "The Prohibition", 12, "Medinan"),
            SurahMeta(67, "Al-Mulk", "الملك", "The Sovereignty", 30, "Meccan"),
            SurahMeta(68, "Al-Qalam", "القلم", "The Pen", 52, "Meccan"),
            SurahMeta(69, "Al-Haqqah", "الحاقة", "The Reality", 52, "Meccan"),
            SurahMeta(70, "Al-Ma'arij", "المعارج", "The Ascending Stairways", 44, "Meccan"),
            SurahMeta(71, "Nuh", "نوح", "Noah", 28, "Meccan"),
            SurahMeta(72, "Al-Jinn", "الجن", "The Jinn", 28, "Meccan"),
            SurahMeta(73, "Al-Muzzammil", "المزمل", "The Enshrouded One", 20, "Meccan"),
            SurahMeta(74, "Al-Muddaththir", "المدثر", "The Cloaked One", 56, "Meccan"),
            SurahMeta(75, "Al-Qiyamah", "القيامة", "The Resurrection", 40, "Meccan"),
            SurahMeta(76, "Al-Insan", "الإنسان", "Man", 31, "Medinan"),
            SurahMeta(77, "Al-Mursalat", "المرسلات", "The Emissaries", 50, "Meccan"),
            SurahMeta(78, "An-Naba", "النبأ", "The Tidings", 40, "Meccan"),
            SurahMeta(79, "An-Nazi'at", "النازعات", "Those who drag forth", 46, "Meccan"),
            SurahMeta(80, "'Abasa", "عبس", "He Frowned", 42, "Meccan"),
            SurahMeta(81, "At-Takwir", "التكوير", "The Overthrowing", 29, "Meccan"),
            SurahMeta(82, "Al-Infitar", "الانفطار", "The Cleaving", 19, "Meccan"),
            SurahMeta(83, "Al-Mutaffifin", "المطففين", "The Defrauders", 36, "Meccan"),
            SurahMeta(84, "Al-Inshiqaq", "الانشقاق", "The Sundering", 25, "Meccan"),
            SurahMeta(85, "Al-Buruj", "البروج", "The Mansions of the Stars", 22, "Meccan"),
            SurahMeta(86, "At-Tariq", "الطارق", "The Night-Comer", 17, "Meccan"),
            SurahMeta(87, "Al-A'la", "الأعلى", "The Most High", 19, "Meccan"),
            SurahMeta(88, "Al-Ghashiyah", "الغاشية", "The Overwhelming", 26, "Meccan"),
            SurahMeta(89, "Al-Fajr", "الفجر", "The Dawn", 30, "Meccan"),
            SurahMeta(90, "Al-Balad", "البلد", "The City", 20, "Meccan"),
            SurahMeta(91, "Ash-Shams", "الشمس", "The Sun", 15, "Meccan"),
            SurahMeta(92, "Al-Layl", "الليل", "The Night", 21, "Meccan"),
            SurahMeta(93, "Ad-Duha", "الضحى", "The Morning Hours", 11, "Meccan"),
            SurahMeta(94, "Ash-Sharh", "الشرح", "The Relief", 8, "Meccan"),
            SurahMeta(95, "At-Tin", "التين", "The Fig", 8, "Meccan"),
            SurahMeta(96, "Al-'Alaq", "العلق", "The Clot", 19, "Meccan"),
            SurahMeta(97, "Al-Qadr", "القدر", "The Power", 5, "Meccan"),
            SurahMeta(98, "Al-Bayyinah", "البينة", "The Clear Proof", 8, "Medinan"),
            SurahMeta(99, "Az-Zalzalah", "الزلزلة", "The Earthquake", 8, "Medinan"),
            SurahMeta(100, "Al-'Adiyat", "العاديات", "The Courser", 11, "Meccan"),
            SurahMeta(101, "Al-Qari'ah", "القارعة", "The Calamity", 11, "Meccan"),
            SurahMeta(102, "At-Takathur", "التكاثر", "The Rivalry", 8, "Meccan"),
            SurahMeta(103, "Al-'Asr", "العصر", "The Declining Day", 3, "Meccan"),
            SurahMeta(104, "Al-Humazah", "الهمزة", "The Traducer", 9, "Meccan"),
            SurahMeta(105, "Al-Fil", "الفيل", "The Elephant", 5, "Meccan"),
            SurahMeta(106, "Quraysh", "قريش", "Quraysh", 4, "Meccan"),
            SurahMeta(107, "Al-Ma'un", "الماعون", "The Small Kindnesses", 7, "Meccan"),
            SurahMeta(108, "Al-Kauthar", "الكوثر", "The Abundance", 3, "Meccan"),
            SurahMeta(109, "Al-Kafirun", "الكافرون", "The Disbelievers", 6, "Meccan"),
            SurahMeta(110, "An-Nasr", "النصر", "The Divine Support", 3, "Medinan"),
            SurahMeta(111, "Al-Masad", "المسد", "The Palm Fiber", 5, "Meccan"),
            SurahMeta(112, "Al-Ikhlas", "الإخلاص", "Purity", 4, "Meccan"),
            SurahMeta(113, "Al-Falaq", "الفلق", "Daybreak", 5, "Meccan"),
            SurahMeta(114, "Al-Nas", "الناس", "Mankind", 6, "Meccan")
        )
    }

    private data class SurahMeta(
        val number: Int,
        val nameEnglish: String,
        val nameArabic: String,
        val translation: String,
        val versesCount: Int,
        val type: String
    )
}
