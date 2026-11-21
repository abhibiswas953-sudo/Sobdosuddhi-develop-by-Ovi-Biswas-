package com.example.engine

import com.example.core.BengaliTextUtils
import com.example.data.model.AiWritingReport
import com.example.data.model.GrammarIssue
import com.example.data.model.ManuscriptCategory
import com.example.data.model.SpellIssue
import com.example.data.model.WritingStyle
import java.util.UUID

/**
 * Offline rule-based Bengali proofreading & stylometric engine.
 * Works with zero internet connection, respects the user's custom dictionary,
 * and avoids flagging intentional regional/poetic/sadhu words as errors.
 */
object BengaliRuleEngine {

    private data class SpellingRule(
        val wrong: String,
        val correct: String,
        val issueType: String,
        val reason: String
    )

    // High-confidence, non-ambiguous Bengali orthographic error dictionary
    private val commonSpellingRules: List<SpellingRule> = listOf(
        // ই-কার / ঈ-কার
        SpellingRule("শ্রদ্ধাঞ্জলী", "শ্রদ্ধাঞ্জলি", "ই-কার ও ঈ-কারের ভুল", "‘অঞ্জলি’ যুক্ত শব্দে হ্রস্ব-ই কার (ি) শুদ্ধ।"),
        SpellingRule("পুষ্পাঞ্জলী", "পুষ্পাঞ্জলি", "ই-কার ও ঈ-কারের ভুল", "‘অঞ্জলি’ শব্দের বানানে হ্রস্ব-ই কার হয়।"),
        SpellingRule("গীতাঞ্জলী", "গীতাঞ্জলি", "ই-কার ও ঈ-কারের ভুল", "প্রমিত বানান ‘গীতাঞ্জলি’।"),
        SpellingRule("পিপিলিকা", "পিপীলিকা", "ই-কার ও ঈ-কারের ভুল", "তৎসম শব্দ ‘পিপীলিকা’-য় দ্বিতীয় প-এ দীর্ঘ-ঈ কার।"),
        SpellingRule("মরিচিকা", "মরীচিকা", "ই-কার ও ঈ-কারের ভুল", "শুদ্ধ তৎসম বানান ‘মরীচিকা’।"),
        SpellingRule("শারিরীক", "শারীরিক", "ই-কার ও ঈ-কারের ভুল", "‘শরীর + ইক = শারীরিক’ (ী-ি ক্রম)।"),
        SpellingRule("শারিরিক", "শারীরিক", "ই-কার ও ঈ-কারের ভুল", "প্রথম র-এ দীর্ঘ-ঈ কার: ‘শারীরিক’।"),
        SpellingRule("প্রতিযোগীতা", "প্রতিযোগিতা", "ই-কার ও ঈ-কারের ভুল", "‘-তা’ প্রত্যয় যুক্ত হলে দীর্ঘ-ঈ হ্রস্ব-ই হয়: ‘প্রতিযোগিতা’।"),
        SpellingRule("সহযোগীতা", "সহযোগিতা", "ই-কার ও ঈ-কারের ভুল", "‘-তা’ প্রত্যয়ের আগে হ্রস্ব-ই কার: ‘সহযোগিতা’।"),
        SpellingRule("উপযোগীতা", "উপযোগিতা", "ই-কার ও ঈ-কারের ভুল", "‘-তা’ প্রত্যয় যুক্ত হয়ে ‘উপযোগিতা’ হয়।"),
        SpellingRule("মনোযোগীতা", "মনোযোগিতা", "ই-কার ও ঈ-কারের ভুল", "‘-তা’ প্রত্যয়ের আগে হ্রস্ব-ই কার শুদ্ধ।"),
        SpellingRule("কিংবদন্তী", "কিংবদন্তি", "ই-কার ও ঈ-কারের ভুল", "প্রমিত বানান ‘কিংবদন্তি’।"),
        SpellingRule("শ্রেণী", "শ্রেণি", "ই-কার ও ঈ-কারের ভুল", "বাংলা একাডেমির প্রমিত বানান ‘শ্রেণি’।"),
        SpellingRule("সরনী", "সরণি", "ই-কার ও ঈ-কারের ভুল", "প্রমিত বানান ‘সরণি’।"),
        SpellingRule("কাহিনী", "কাহিনি", "ই-কার ও ঈ-কারের ভুল", "আধুনিক প্রমিত বাংলা বানানে ‘কাহিনি’।"),
        SpellingRule("জরীপ", "জরিপ", "ই-কার ও ঈ-কারের ভুল", "বিদেশি/অতৎসম শব্দে হ্রস্ব-ই কার: ‘জরিপ’।"),
        SpellingRule("শহীদ", "শহিদ", "ই-কার ও ঈ-কারের ভুল", "অতৎসম শব্দে হ্রস্ব-ই কার প্রমিত (সাহিত্যিক ঐতিহ্যে ‘শহীদ’ও প্রচলিত)।"),
        SpellingRule("গভির", "গভীর", "ই-কার ও ঈ-কারের ভুল", "তৎসম শব্দে দীর্ঘ-ঈ কার: ‘গভীর’।"),
        SpellingRule("নিরব", "নীরব", "ই-কার ও ঈ-কারের ভুল", "নিঃ + রব = নীরব (দীর্ঘ-ঈ কার)।"),
        SpellingRule("নিরবতা", "নীরবতা", "ই-কার ও ঈ-কারের ভুল", "‘নীরব’ থেকে গঠিত, তাই দীর্ঘ-ঈ কার: ‘নীরবতা’।"),
        SpellingRule("নিরস", "নীরস", "ই-কার ও ঈ-কারের ভুল", "নিঃ + রস = নীরস।"),
        SpellingRule("নিরোগ", "নীরোগ", "ই-কার ও ঈ-কারের ভুল", "নিঃ + রোগ = নীরোগ।"),
        SpellingRule("আশির্বাদ", "আশীর্বাদ", "ই-কার ও রেফের ভুল", "সন্ধিজাত তৎসম বানান ‘আশীর্বাদ’ (শ-এ ঈ-কার ও ব-এ রেফ)।"),
        SpellingRule("আর্শীবাদ", "আশীর্বাদ", "রেফের অবস্থানগত ভুল", "রেফটি ‘শ’-এর ওপর নয়, ‘ব’-এর ওপর বসবে: ‘আশীর্বাদ’।"),

        // উ-কার / ঊ-কার
        SpellingRule("মুহুর্ত", "মুহূর্ত", "উ-কার ও ঊ-কারের ভুল", "‘মুহূর্ত’ বানানে হ-এ দীর্ঘ-ঊ কার (ূ)।"),
        SpellingRule("মূহুর্ত", "মুহূর্ত", "উ-কার ও ঊ-কারের ভুল", "ম-এ হ্রস্ব-উ কার ও হ-এ দীর্ঘ-ঊ কার: ‘মুহূর্ত’।"),
        SpellingRule("মূহূর্ত", "মুহূর্ত", "উ-কার ও ঊ-কারের ভুল", "প্রথম ম-এ হ্রস্ব-উ কার: ‘মুহূর্ত’।"),
        SpellingRule("নূন্যতম", "ন্যূনতম", "য-ফলা ও ঊ-কারের ভুল", "শুদ্ধ বানান ‘ন্যূনতম’ (ন+য-ফলা+ঊ-কার)।"),
        SpellingRule("ন্যুনতম", "ন্যূনতম", "উ-কার ও ঊ-কারের ভুল", "‘ন্যূন’ শব্দে দীর্ঘ-ঊ কার হয়: ‘ন্যূনতম’।"),
        SpellingRule("দুরত্ব", "দূরত্ব", "উ-কার ও ঊ-কারের ভুল", "‘দূর’ শব্দে দীর্ঘ-ঊ কার: ‘দূরত্ব’।"),
        SpellingRule("ভুল", "ভুল", "", ""), // sentinel ignored
        SpellingRule("ভূল", "ভুল", "উ-কার ও ঊ-কারের ভুল", "বাংলা ‘ভুল’ বানানে হ্রস্ব-উ কার (ু) শুদ্ধ।"),
        SpellingRule("পুজো", "পুজো", "", ""),
        SpellingRule("পূজা", "পূজা", "", ""),
        SpellingRule("পুজা", "পূজা", "উ-কার ও ঊ-কারের ভুল", "তৎসম বানান ‘পূজা’ (দীর্ঘ-ঊ কার)।"),
        SpellingRule("মুমুর্ষু", "মুমূর্ষু", "উ-কার ও ঊ-কারের ভুল", "শুদ্ধ বানান ‘মুমূর্ষু’ (ু-ূ-ু ক্রম)।"),
        SpellingRule("মুমূর্ষ", "মুমূর্ষু", "উ-কার ও ঊ-কারের ভুল", "শেষে হ্রস্ব-উ কার যুক্ত হয়ে ‘মুমূর্ষু’ হয়।"),
        SpellingRule("শূন্যতা", "শূন্যতা", "", ""),
        SpellingRule("শুন্য", "শূন্য", "উ-কার ও ঊ-কারের ভুল", "তৎসম ‘শূন্য’ বানানে দীর্ঘ-ঊ কার।"),
        SpellingRule("শুন্যতা", "শূন্যতা", "উ-কার ও ঊ-কারের ভুল", "‘শূন্য’ থেকে গঠিত, তাই ‘শূন্যতা’।"),

        // শ, ষ, স
        SpellingRule("পুরষ্কার", "পুরস্কার", "শ, ষ ও স-এর ভুল", "অ-কারের পর বিসর্গ সন্ধিতে দন্ত্য-স হয়: ‘পুরস্কার’।"),
        SpellingRule("নমষ্কার", "নমস্কার", "শ, ষ ও স-এর ভুল", "নমঃ + কার = নমস্কার (দন্ত্য-স)।"),
        SpellingRule("তিরষ্কার", "তিরস্কার", "শ, ষ ও স-এর ভুল", "তিরঃ + কার = তিরস্কার (দন্ত্য-স)।"),
        SpellingRule("আবিস্কার", "আবিষ্কার", "শ, ষ ও স-এর ভুল", "ই-কারের পর বিসর্গ সন্ধিতে মূর্ধন্য-ষ হয়: ‘আবিষ্কার’।"),
        SpellingRule("পরিস্কার", "পরিষ্কার", "শ, ষ ও স-এর ভুল", "পরিঃ + কার = পরিষ্কার (মূর্ধন্য-ষ)।"),
        SpellingRule("বহিস্কার", "বহিষ্কার", "শ, ষ ও স-এর ভুল", "বহিঃ + কার = বহিষ্কার (মূর্ধন্য-ষ)।"),
        SpellingRule("ভবিষ্যত", "ভবিষ্যৎ", "খণ্ড-ত ও বানান ভুল", "শুদ্ধ তৎসম বানান ‘ভবিষ্যৎ’।"),
        SpellingRule("জিনিষ", "জিনিস", "শ, ষ ও স-এর ভুল", "অতৎসম শব্দে দন্ত্য-স: ‘জিনিস’।"),
        SpellingRule("পোষ্ট", "পোস্ট", "শ, ষ ও স-এর ভুল", "বিদেশি শব্দে ‘স্ট’ বসে, ‘ষ্ট’ নয়: ‘পোস্ট’।"),
        SpellingRule("মাষ্টার", "মাস্টার", "শ, ষ ও স-এর ভুল", "বিদেশি শব্দে দন্ত্য-স: ‘মাস্টার’।"),
        SpellingRule("স্টেশন", "স্টেশন", "", ""),
        SpellingRule("ষ্টেশন", "স্টেশন", "শ, ষ ও স-এর ভুল", "বিদেশি শব্দে ‘স্ট’ শুদ্ধ: ‘স্টেশন’।"),
        SpellingRule("সরস্বতি", "সরস্বতী", "ই-কার ও ব-ফলার ভুল", "তৎসম বানান ‘সরস্বতী’ (দীর্ঘ-ঈ কার)।"),

        // ণ ও ন
        SpellingRule("ধরণ", "ধরন", "ণ ও ন-এর ব্যবহার", "প্রকার বা রীতি অর্থে ‘ধরন’ (দন্ত্য-ন) শুদ্ধ (ধারণ অর্থে ‘ধরণ’)।"),
        SpellingRule("কর্ণেল", "কর্নেল", "ণ ও ন-এর ব্যবহার", "বিদেশি শব্দে ণ-ত্ব বিধান প্রযোজ্য নয়: ‘কর্নেল’।"),
        SpellingRule("গভর্ণর", "গভর্নর", "ণ ও ন-এর ব্যবহার", "বিদেশি শব্দে দন্ত্য-ন বসে: ‘গভর্নর’।"),
        SpellingRule("ফটোষ্ট্যাট", "ফটোস্ট্যাট", "শ, ষ ও স-এর ভুল", "বিদেশি শব্দে ‘স্ট’ ব্যবহৃত হয়।"),
        SpellingRule("রুপায়ন", "রূপায়ণ", "ঊ-কার ও ণ-এর ভুল", "‘রূপ + অয়ন = রূপায়ণ’ (দীর্ঘ-ঊ কার ও মূর্ধন্য-ণ)।"),
        SpellingRule("রামায়ন", "রামায়ণ", "ণ ও ন-এর ব্যবহার", "র-এর পর অয়ন যুক্ত হয়ে মূর্ধন্য-ণ: ‘রামায়ণ’।"),
        SpellingRule("ব্যাঙ", "ব্যাঙ", "", ""),
        SpellingRule("ব্রাহ্মন", "ব্রাহ্মণ", "ণ ও ন-এর ব্যবহার", "র-ফলার পর ণ-ত্ব বিধানে ‘ব্রাহ্মণ’।"),
        SpellingRule("বর্ননা", "বর্ণনা", "ণ ও ন-এর ব্যবহার", "রেফের পর মূর্ধন্য-ণ বসে: ‘বর্ণনা’।"),
        SpellingRule("প্রান", "প্রাণ", "ণ ও ন-এর ব্যবহার", "র-ফলার পর তৎসম শব্দে মূর্ধন্য-ণ: ‘প্রাণ’।"),
        SpellingRule("করুনা", "করুণা", "ণ ও ন-এর ব্যবহার", "র-এর পর তৎসম শব্দে মূর্ধন্য-ণ: ‘করুণা’।"),

        // রেফ, র-ফলা, য-ফলা, ব-ফলা ও যুক্তাক্ষর
        SpellingRule("কার্য্য", "কার্য", "রেফ ও দ্বিত্বের ভুল", "প্রমিত বাংলায় রেফের পর ব্যঞ্জনদ্বিত্ব বর্জনীয়: ‘কার্য’।"),
        SpellingRule("সূর্য্য", "সূর্য", "রেফ ও দ্বিত্বের ভুল", "রেফের পর দ্বিত্ব হয় না: ‘সূর্য’।"),
        SpellingRule("ধর্ম্ম", "ধর্ম", "রেফ ও দ্বিত্বের ভুল", "রেফের পর দ্বিত্ব বর্জনীয়: ‘ধর্ম’।"),
        SpellingRule("সংবর্দ্ধনা", "সংবর্ধনা", "রেফ ও দ্বিত্বের ভুল", "প্রমিত বানান ‘সংবর্ধনা’।"),
        SpellingRule("স্বরন", "স্মরণ", "যুক্তাক্ষরের ভুল", "মনে করা অর্থে ‘স্মরণ’ (স+ম-ফলা ও ণ)।"),
        SpellingRule("শ্মশান", "শ্মশান", "", ""),
        SpellingRule("স্মশান", "শ্মশান", "যুক্তাক্ষরের ভুল", "শুদ্ধ বানান ‘শ্মশান’ (তালব্য শ+ম-ফলা)।"),
        SpellingRule("সান্তনা", "সান্ত্বনা", "ব-ফলার ভুল", "তৎসম বানানে ত-এ ব-ফলা আছে: ‘সান্ত্বনা’।"),
        SpellingRule("শান্তনা", "সান্ত্বনা", "শ/স ও ব-ফলার ভুল", "শুদ্ধ বানান ‘সান্ত্বনা’।"),
        SpellingRule("উচ্ছাস", "উচ্ছ্বাস", "ব-ফলার ভুল", "উৎ + শ্বাস = উচ্ছ্বাস (ছ-এ ব-ফলা)।"),
        SpellingRule("উজ্জল", "উজ্জ্বল", "ব-ফলার ভুল", "উৎ + জ্বল = উজ্জ্বল (জ+জ+ব-ফলা)।"),
        SpellingRule("প্রজ্জ্বলিত", "প্রজ্বলিত", "যুক্তাক্ষরের ভুল", "প্র + জ্বলিত = প্রজ্বলিত (একটি জ)।"),
        SpellingRule("দ্বন্দ", "দ্বন্দ্ব", "ব-ফলার ভুল", "দুই দ-এ ব-ফলা: ‘দ্বন্দ্ব’।"),
        SpellingRule("অধ্যায়ন", "অধ্যয়ন", "আ-কারের ভুল", "অধি + অয়ন = অধ্যয়ন (য-এ আ-কার হবে না)।"),
        SpellingRule("ব্যাথা", "ব্যথা", "য-ফলা ও আ-কারের ভুল", "‘ব্যথা’ বানানে য-ফলার পর আ-কার হয় না।"),
        SpellingRule("ব্যাতীত", "ব্যতীত", "য-ফলা ও আ-কারের ভুল", "বি + অতীত = ব্যতীত (আ-কার হবে না)।"),
        SpellingRule("ব্যার্থ", "ব্যর্থ", "য-ফলা ও আ-কারের ভুল", "বি + অর্থ = ব্যর্থ (আ-কার হবে না)।"),
        SpellingRule("ব্যাবহার", "ব্যবহার", "য-ফলা ও আ-কারের ভুল", "বি + অবহার = ব্যবহার (আ-কার হবে না)।"),
        SpellingRule("সন্ন্যাসি", "সন্ন্যাসী", "ই-কার ও ঈ-কারের ভুল", "শুদ্ধ বানান ‘সন্ন্যাসী’।"),
        SpellingRule("স্বায়ত্বশাসন", "স্বায়ত্তশাসন", "ব-ফলার ভুল", "স্ব + আয়ত্ত + শাসন = স্বায়ত্তশাসন (ত+ত, ব-ফলা নয়)।"),
        SpellingRule("দারিদ্রতা", "দারিদ্র্য", "প্রত্যয়ের ভুল", "‘দরিদ্রতা’ অথবা ‘দারিদ্র্য’ শুদ্ধ; ‘দারিদ্রতা’ অশুদ্ধ।"),
        SpellingRule("সৌজন্যতা", "সৌজন্য", "প্রত্যয়ের ভুল", "‘সৌজন্য’ নিজেই ভাববাচক বিশেষ্য; ‘-তা’ যোগ করা বাহুল্য।"),
        SpellingRule("সফলতা", "সাফল্য", "শব্দগঠন পরামর্শ", "তৎসম প্রমিত রূপ ‘সাফল্য’ (সফলতা প্রচলিত হলেও ‘সাফল্য’ মার্জিত)।"),
        SpellingRule("ইতিমধ্যে", "ইতোমধ্যে", "বিসর্গ সন্ধির ভুল", "ইতঃ + মধ্যে = ইতোমধ্যে।"),
        SpellingRule("ইতিপূর্বে", "ইতোপূর্বে", "বিসর্গ সন্ধির ভুল", "ইতঃ + পূর্বে = ইতোপূর্বে।"),
        SpellingRule("নূন্য", "ন্যূন", "য-ফলা ও ঊ-কারের ভুল", "শুদ্ধ বানান ‘ন্যূন’।")
    ).filter { it.wrong != it.correct && it.issueType.isNotBlank() }

    private val redundantPhraseRules: List<SpellingRule> = listOf(
        SpellingRule("শুধুমাত্র", "শুধু", "অপ্রয়োজনীয় শব্দ / বাহুল্য", "‘শুধু’ ও ‘মাত্র’ সমার্থক; একসঙ্গে ব্যবহার বাহুল্যদোষ।"),
        SpellingRule("কেবলমাত্র", "কেবল", "অপ্রয়োজনীয় শব্দ / বাহুল্য", "‘কেবল’ ও ‘মাত্র’ সমার্থক; যেকোনো একটি ব্যবহার করুন।"),
        SpellingRule("অত্যন্ত খুবই", "অত্যন্ত", "অপ্রয়োজনীয় শব্দ / বাহুল্য", "দুটি তীব্রতাবাচক অব্যয় পাশাপাশি বসেছে।"),
        SpellingRule("আবার পুনরায়", "পুনরায়", "অপ্রয়োজনীয় শব্দ / বাহুল্য", "‘আবার’ এবং ‘পুনরায়’ একই অর্থ বহন করে।"),
        SpellingRule("একত্রিত হয়ে", "একত্র হয়ে", "প্রত্যয়ের বাহুল্য", "‘একত্র হয়ে’ অথবা ‘একত্রিত’ — যেকোনো একটি প্রয়োগ শুদ্ধ।"),
        SpellingRule("সকল মানুষগণ", "সকল মানুষ", "বহুবচনের বাহুল্য", "একই পদে দুইবার বহুবচন ব্যবহার করা হয়েছে।"),
        SpellingRule("সব ছাত্রবৃন্দ", "ছাত্রবৃন্দ", "বহুবচনের বাহুল্য", "‘সব’ ও ‘বৃন্দ’ একসঙ্গে ব্যবহার বাহুল্যদোষ।"),
        SpellingRule("বিনা অনুমতিতে ব্যতীত", "অনুমতি ব্যতীত", "অপ্রয়োজনীয় শব্দ", "‘বিনা’ ও ‘ব্যতীত’ একসঙ্গে পুনরুক্তি ঘটায়।")
    )

    private val sadhuMarkers = setOf(
        "করিয়া", "যাইয়া", "হইয়া", "বলিয়া", "থাকিয়া", "দেখিয়া", "আসিয়া", "গিয়া",
        "করিয়াছে", "করিয়াছেন", "করিয়াছিল", "করিয়াছিলেন",
        "যাইতেছে", "যাইতেছেন", "যাইতেছিল", "যাইতেছিলেন",
        "করিতেছে", "করিতেছেন", "করিতেছিল", "করিতেছিলেন",
        "হইতেছে", "হইতেছেন", "হইতেছিল", "হইতেছিলেন",
        "বলিতেছে", "বলিতেছেন", "বলিয়াছে", "বলিয়াছেন",
        "দেখিতেছে", "দেখিতেছেন", "দেখিয়াছে", "দেখিয়াছেন",
        "গিয়াছে", "গিয়াছেন", "আসিয়াছে", "আসিয়াছেন",
        "তাহার", "তাহাকে", "তাহারা", "তাহাদের", "ইহার", "উহার", "যাহা", "তাহা"
    )
    private val cholitoMarkers = setOf(
        "করে", "গিয়ে", "হয়ে", "বলে", "থেকে", "দেখে", "এসে",
        "করেছে", "করেছেন", "করেছিল", "করেছিলেন", "করলেন",
        "যাচ্ছে", "যাচ্ছেন", "যাচ্ছিল", "যাচ্ছিলেন", "গেলেন",
        "করছে", "করছেন", "করছিল", "করছিলেন",
        "হচ্ছে", "হচ্ছেন", "হচ্ছিল", "হচ্ছিলেন",
        "বলছে", "বলছেন", "বলেছে", "বলেছেন", "বললেন",
        "দেখছে", "দেখছেন", "দেখেছে", "দেখেছেন", "এলেন",
        "তার", "তাকে", "তারা", "তাদের", "এর", "ওর", "যা", "তা"
    )

    fun analyzeOfflineSpellAndRules(
        text: String,
        customDictionary: Set<String>,
        writingStyle: WritingStyle,
        category: ManuscriptCategory
    ): List<SpellIssue> {
        if (text.isBlank()) return emptyList()
        val issues = mutableListOf<SpellIssue>()
        val normalizedDict = customDictionary.map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        // 1. Dictionary-aware word spelling scan
        val wordRegex = Regex("[\\u0980-\\u09FF]+")
        val matches = wordRegex.findAll(text).toList()

        val ruleMap = commonSpellingRules.associateBy { it.wrong }
        val seenWordOffsets = mutableSetOf<String>()

        for (match in matches) {
            val word = match.value
            if (word in normalizedDict) continue
            // Respect poetic license on softer style notes
            val rule = ruleMap[word]
            if (rule != null) {
                if (category == ManuscriptCategory.POETRY && rule.wrong in setOf("শহীদ", "সফলতা", "ধরণ")) {
                    continue
                }
                val key = "${rule.wrong}_${match.range.first}"
                if (seenWordOffsets.add(key)) {
                    issues.add(
                        SpellIssue(
                            id = UUID.randomUUID().toString(),
                            original = rule.wrong,
                            issueType = rule.issueType,
                            suggestion = rule.correct,
                            reason = rule.reason,
                            source = "RULE",
                            startIndex = match.range.first
                        )
                    )
                }
            }
        }

        // 2. Redundant phrase scan (skipped in Poetry unless blatantly redundant)
        if (category != ManuscriptCategory.POETRY) {
            for (phraseRule in redundantPhraseRules) {
                if (phraseRule.wrong in normalizedDict) continue
                val idx = text.indexOf(phraseRule.wrong)
                if (idx >= 0) {
                    issues.add(
                        SpellIssue(
                            id = UUID.randomUUID().toString(),
                            original = phraseRule.wrong,
                            issueType = phraseRule.issueType,
                            suggestion = phraseRule.correct,
                            reason = phraseRule.reason,
                            source = "RULE",
                            startIndex = idx
                        )
                    )
                }
            }
        }

        // 3. Consecutive duplicate word detection (e.g., "কথা কথা" — skipping poetic reduplications like ধীরে ধীরে, ছোট ছোট)
        val poeticReduplications = setOf(
            "ধীরে", "ছোট", "বড়", "লাল", "নীল", "সবুজ", "নরম", "হালকা", "মৃদু", "নতুন",
            "ঘন", "টুপ", "ঝিরি", "রিনি", "টিপ", "শন্", "দূর", "আস্তে", "মাঝে", "কখনো",
            "বার", "ঘরে", "পথে", "জনে", "মনে", "চোখে", "হাতে", "দিনে", "রাতে", "ফোঁটা", "পাতা"
        )
        for (i in 0 until matches.lastIndex) {
            val first = matches[i]
            val second = matches[i + 1]
            if (first.value == second.value &&
                first.value !in normalizedDict &&
                first.value !in poeticReduplications &&
                first.value.length >= 2
            ) {
                val between = text.substring(first.range.last + 1, second.range.first)
                if (between.isNotEmpty() && between.all { it == ' ' }) {
                    val combined = "${first.value} ${second.value}"
                    issues.add(
                        SpellIssue(
                            id = UUID.randomUUID().toString(),
                            original = combined,
                            issueType = "শব্দের পুনরাবৃত্তি",
                            suggestion = first.value,
                            reason = "একই শব্দ পরপর দুইবার ব্যবহৃত হয়েছে (ইচ্ছাকৃত দ্বিরুক্তি না হলে একটি বাদ দিন)।",
                            source = "RULE",
                            startIndex = first.range.first
                        )
                    )
                }
            }
        }

        // 4. Punctuation checks: space before দাঁড়ি/কমা, double দাঁড়ি, ASCII period after Bengali letters
        val spaceBeforePunct = Regex("([\\u0980-\\u09FF]+)\\s+([।,?!;])")
        spaceBeforePunct.findAll(text).take(6).forEach { m ->
            issues.add(
                SpellIssue(
                    id = UUID.randomUUID().toString(),
                    original = m.value,
                    issueType = "বিরামচিহ্নের ভুল",
                    suggestion = "${m.groupValues[1]}${m.groupValues[2]}",
                    reason = "বিরামচিহ্নের আগে অপ্রয়োজনীয় স্পেস বসে না।",
                    source = "RULE",
                    startIndex = m.range.first
                )
            )
        }

        val doubleDari = Regex("।।+")
        doubleDari.findAll(text).take(5).forEach { m ->
            issues.add(
                SpellIssue(
                    id = UUID.randomUUID().toString(),
                    original = m.value,
                    issueType = "বিরামচিহ্নের ভুল",
                    suggestion = "।",
                    reason = "বাক্যের শেষে একাধিক দাঁড়ি ব্যবহৃত হয়েছে।",
                    source = "RULE",
                    startIndex = m.range.first
                )
            )
        }

        // ASCII period attached to Bengali word (skip ellipses "...")
        val asciiPeriodAfterBengali = Regex("([\\u0980-\\u09FF]+)\\.(?!\\.)")
        asciiPeriodAfterBengali.findAll(text).take(5).forEach { m ->
            issues.add(
                SpellIssue(
                    id = UUID.randomUUID().toString(),
                    original = m.value,
                    issueType = "বিরামচিহ্নের ভুল",
                    suggestion = "${m.groupValues[1]}।",
                    reason = "বাংলা বাক্যের শেষে ইংরেজি ফুলস্টপ (.)-এর বদলে বাংলা দাঁড়ি (।) ব্যবহার করা উচিত।",
                    source = "RULE",
                    startIndex = m.range.first
                )
            )
        }

        return issues.distinctBy { "${it.original}_${it.startIndex}" }
    }

    fun analyzeOfflineGrammar(
        text: String,
        writingStyle: WritingStyle,
        category: ManuscriptCategory
    ): List<GrammarIssue> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<GrammarIssue>()
        val sentences = text.split(Regex("(?<=[।?!\\n])")).map { it.trim() }.filter { it.length >= 6 }

        for (sentence in sentences.take(15)) {
            val words = Regex("[\\u0980-\\u09FF]+").findAll(sentence).map { it.value }.toList()
            val hasSadhu = words.any { it in sadhuMarkers }
            val hasCholito = words.any { it in cholitoMarkers }

            if (hasSadhu && hasCholito && category != ManuscriptCategory.POETRY) {
                val cholitoFix = sentence
                    .replace("করিয়াছিলেন", "করেছিলেন")
                    .replace("করিয়াছেন", "করেছেন")
                    .replace("করিয়াছিল", "করেছিল")
                    .replace("করিয়াছে", "করেছে")
                    .replace("যাইতেছিলেন", "যাচ্ছিলেন")
                    .replace("যাইতেছেন", "যাচ্ছেন")
                    .replace("যাইতেছিল", "যাচ্ছিল")
                    .replace("যাইতেছে", "যাচ্ছে")
                    .replace("করিতেছিলেন", "করছিলেন")
                    .replace("করিতেছেন", "করছেন")
                    .replace("করিতেছিল", "করছিল")
                    .replace("করিতেছে", "করছে")
                    .replace("হইতেছিলেন", "হচ্ছিলেন")
                    .replace("হইতেছেন", "হচ্ছেন")
                    .replace("হইতেছিল", "হচ্ছিল")
                    .replace("হইতেছে", "হচ্ছে")
                    .replace("বলিতেছেন", "বলছেন")
                    .replace("বলিতেছে", "বলছে")
                    .replace("বলিয়াছেন", "বলেছেন")
                    .replace("বলিয়াছে", "বলেছে")
                    .replace("দেখিতেছেন", "দেখছেন")
                    .replace("দেখিয়াছেন", "দেখেছেন")
                    .replace("গিয়াছেন", "গিয়েছেন")
                    .replace("আসিয়াছেন", "এসেছেন")
                    .replace("করিয়া", "করে")
                    .replace("যাইয়া", "গিয়ে")
                    .replace("হইয়া", "হয়ে")
                    .replace("বলিয়া", "বলে")
                    .replace("দেখিয়া", "দেখে")
                    .replace("তাহাদের", "তাদের")
                    .replace("তাহারা", "তারা")
                    .replace("তাহার", "তার")
                    .replace("তাহাকে", "তাকে")
                val sadhuFix = sentence
                    .replace("করেছিলেন", "করিয়াছিলেন")
                    .replace("করেছেন", "করিয়াছেন")
                    .replace("করেছে", "করিয়াছে")
                    .replace("যাচ্ছেন", "যাইতেছেন")
                    .replace("যাচ্ছে", "যাইতেছে")
                    .replace("করছেন", "করিতেছেন")
                    .replace("করছে", "করিতেছে")
                    .replace("হচ্ছেন", "হইতেছেন")
                    .replace("হচ্ছে", "হইতেছে")
                    .replace("বলছেন", "বলিতেছেন")
                    .replace("বলেছেন", "বলিয়াছেন")
                    .replace("বলেছে", "বলিয়াছে")

                results.add(
                    GrammarIssue(
                        id = UUID.randomUUID().toString(),
                        originalSentence = sentence,
                        issueType = "সাধু ও চলিত রীতির মিশ্রণ",
                        reason = "একই বাক্যে সাধু ও চলিত ভাষার ক্রিয়াপদ/সর্বনাম মিশে গেছে। নির্বাচিত রীতি (${writingStyle.labelBn}) বজায় রাখলে বাক্যটি আরও স্বাভাবিক হবে।",
                        minimalCorrection = if (writingStyle == WritingStyle.SADHU) sadhuFix else cholitoFix,
                        grammaticalCorrection = if (writingStyle == WritingStyle.SADHU) sadhuFix else cholitoFix,
                        fluentAlternative = if (writingStyle == WritingStyle.SADHU) sadhuFix else cholitoFix,
                        source = "RULE"
                    )
                )
            }

            // Detect excessive "এবং" chaining inside one sentence
            val ebongCount = words.count { it == "এবং" }
            if (ebongCount >= 3) {
                val firstIdx = sentence.indexOf(" এবং ")
                val streamlined = if (firstIdx > 0) {
                    sentence.replaceFirst(" এবং ", ", ")
                } else sentence
                results.add(
                    GrammarIssue(
                        id = UUID.randomUUID().toString(),
                        originalSentence = sentence,
                        issueType = "বাক্যের প্রবাহ ও পুনরাবৃত্তি",
                        reason = "একই বাক্যে ‘এবং’ একাধিকবার ব্যবহৃত হওয়ায় বাক্যের স্বাভাবিক ছন্দ ভারী হয়ে পড়েছে।",
                        minimalCorrection = streamlined,
                        grammaticalCorrection = streamlined,
                        fluentAlternative = streamlined.replaceFirst(" এবং ", " ও "),
                        source = "RULE"
                    )
                )
            }
        }
        return results
    }

    /**
     * Local stylometric AI-writing signal estimator (used offline and as baseline alongside Gemini AI).
     * Never declares absolute authorship; presents cautious statistical signals only.
     */
    fun analyzeOfflineAiSignals(text: String): AiWritingReport {
        val metrics = BengaliTextUtils.computeMetrics(text)
        val sentences = text.split(Regex("[।?!\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
        val sentenceLengths = sentences.map {
            it.split(Regex("\\s+")).count { w -> w.isNotBlank() }
        }

        val meanLen = if (sentenceLengths.isNotEmpty()) sentenceLengths.average() else 0.0
        val variance = if (sentenceLengths.size > 1) {
            sentenceLengths.map { (it - meanLen) * (it - meanLen) }.average()
        } else 12.0
        val stdDev = kotlin.math.sqrt(variance)

        val templateMarkers = listOf(
            "পরিশেষে বলা যায়", "সংক্ষেপে বলা যায়", "অন্যদিকে বলা যায়",
            "এটি অত্যন্ত গুরুত্বপূর্ণ যে", "উল্লেখযোগ্য যে", "সর্বোপরি বলা যায়",
            "নিঃসন্দেহে বলা যায়", "বলা বাহুল্য যে"
        )
        val foundTemplates = templateMarkers.filter { text.contains(it) }

        val words = Regex("[\\u0980-\\u09FF]{3,}").findAll(text).map { it.value }.toList()
        val freq = words.groupingBy { it }.eachCount()
        val overused = freq.entries
            .filter { it.value >= 4 && it.value.toDouble() / words.size.coerceAtLeast(1) > 0.045 }
            .sortedByDescending { it.value }
            .take(5)
            .map { "‘${it.key}’ (${BengaliTextUtils.toBengaliDigits(it.value)} বার)" }

        // Compute cautious heuristic signal score (0..100)
        var signal = 22
        if (sentenceLengths.size >= 4 && stdDev < 2.2) signal += 22
        if (metrics.lexicalDiversityPercent in 1..48 && metrics.wordCount >= 40) signal += 18
        signal += (foundTemplates.size * 14).coerceAtMost(35)
        if (overused.size >= 3) signal += 10
        signal = signal.coerceIn(5, 88)

        val band = when {
            signal < 30 -> "কম সম্ভাব্য AI লক্ষণ (স্বাভাবিক মানবিক বৈচিত্র্য)"
            signal < 58 -> "মাঝারি বা মিশ্র শৈলীগত লক্ষণ"
            else -> "অপেক্ষাকৃত বেশি পুনরাবৃত্ত ও ছকবাঁধা লক্ষণ"
        }

        val uniformityNote = when {
            sentenceLengths.size < 3 -> "বাক্যের সংখ্যা কম হওয়ায় গঠনগত বৈচিত্র্য নির্ণয়ে পর্যাপ্ত নমুনা নেই।"
            stdDev < 2.2 -> "বাক্যগুলোর দৈর্ঘ্য প্রায় একই রকম (গড় ${BengaliTextUtils.toBengaliDigits(meanLen.toInt())} শব্দ), যা যান্ত্রিক বা ছকবাঁধা গদ্যের ইঙ্গিত হতে পারে।"
            else -> "বাক্যের দৈর্ঘ্যে স্বাভাবিক ওঠানামা ও ছন্দ-বৈচিত্র্য রয়েছে।"
        }

        val observations = buildList {
            add("মোট শব্দ: ${BengaliTextUtils.toBengaliDigits(metrics.wordCount)}, স্বতন্ত্র শব্দ: ${BengaliTextUtils.toBengaliDigits(metrics.uniqueWordCount)} (শব্দবৈচিত্র্য ${BengaliTextUtils.toBengaliDigits(metrics.lexicalDiversityPercent)}%)।")
            add(uniformityNote)
            if (foundTemplates.isNotEmpty()) {
                add("ছকবাঁধা বা টেমপ্লেট-ধাঁচের বাক্যাংশ শনাক্ত হয়েছে: ${foundTemplates.joinToString(", ")}।")
            }
            if (overused.isNotEmpty()) {
                add("একই শব্দের উচ্চ পুনরাবৃত্তি: ${overused.joinToString(", ")}।")
            }
        }

        return AiWritingReport(
            aiSignalConfidencePercent = signal,
            signalBandLabel = band,
            naturalnessScore = (100 - signal).coerceIn(15, 95),
            lexicalDiversityScore = metrics.lexicalDiversityPercent,
            sentenceUniformityNote = uniformityNote,
            repetitionSignals = overused,
            templatePhrasingSignals = foundTemplates,
            detailedObservations = observations,
            disclaimer = "সতর্কতা: কোনো AI Detector শতভাগ নির্ভুলভাবে লেখার উৎস শনাক্ত করতে পারে না। এই ফলাফলটি কেবল ভাষাগত ছন্দ, পুনরাবৃত্তি ও গঠনের একটি সম্ভাব্য ইঙ্গিত হিসেবে দেখুন।",
            fromAi = false
        )
    }
}
