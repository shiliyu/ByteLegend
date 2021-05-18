package com.bytelegend.utils

import com.bytelegend.app.shared.i18n.Locale
import com.bytelegend.app.shared.i18n.LocalizedText
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

/**
 * Convert i18n YAMLs to JSONs, with extra work:
 *
 * 1. Automatically translate zh-hans to zh-hant.
 * 2. Merge all data to i18n-all.json for backend.
 *
 * Arguments:
 * 1. Input `game-data` directory
 * 2. Output i18n directory
 * 3. Output i18n-all.json file
 */
fun main(args: Array<String>) {
    generate(File(args[0]), File(args[1]), File(args[2]))
}

class I18nResource(
    // mapId or "common"
    val mapId: String,
    val yamlFile: File
) {
    val autoJsonFile: File = yamlFile.parentFile.resolve(yamlFile.name.replace(".yml", "-auto.json"))
    private val localizedTextsInYaml: LinkedHashMap<String, LocalizedText> by lazy {
        yamlFile.yamlToLocalizedTexts().apply {
            values.forEach(LocalizedText::validate)
        }
    }
    private val localizedTextsInJson: LinkedHashMap<String, LocalizedText> by lazy {
        if (autoJsonFile.isFile) {
            autoJsonFile.jsonToLocalizedTexts()
        } else {
            LinkedHashMap()
        }
    }

    // Merge i18n.yml and i18n-auto.json, with the following rules:
    // For each entry E, language L in i18n.yaml:
    //   We assert en/zh-hans is written by human.
    //   If E/L exists in i18n.yml, use it directly. We trust human unconditionally.
    //   Else if E exists in i18n-auto.json and E's en/zh-hans versions are same as i18n.yml:
    //      Do nothing. We did the translation before and nothing changes.
    //   Else if google cloud translation API is not configured:
    //      1. Copy en version in yml as all other language versions except zh-hans/zh-hant to JSON.
    //      2. Use machine to convert zh-hans to zh-hant
    //   Else invoke google cloud translation API to translate:
    //      1. en to all other language versions as zh-hans/zh-hant
    //      2. zh-hans to zh-hant
    fun generate(): List<LocalizedText> =
        localizedTextsInYaml.map { (textId, i18nTexts) ->
            val localeToText = mutableMapOf<String, String>()
            val enIsSame = i18nTexts.getTextOrNull(Locale.EN) == localizedTextsInJson[textId]?.getTextOrNull(Locale.EN)
            val zhHansIsSame = i18nTexts.getTextOrNull(Locale.ZH_HANS) == localizedTextsInJson[textId]?.getTextOrNull(Locale.ZH_HANS)

            Locale.values().filter { it != Locale.ALL }.forEach { locale ->
                when {
                    i18nTexts.getTextOrNull(locale) != null -> localeToText[locale.toString()] = i18nTexts.getTextOrNull(locale)!!
                    // zh-hans is same and target is zh-hant, just use the translated text in json
                    zhHansIsSame && locale == Locale.ZH_HANT &&
                        localizedTextsInJson[textId]?.getTextOrNull(locale) != null -> localeToText[locale.toString()] = localizedTextsInJson[textId]?.getTextOrNull(locale)!!
                    // en is same, just use the translated text in json
                    enIsSame && localizedTextsInJson[textId]?.getTextOrNull(locale) != null -> localeToText[locale.toString()] = localizedTextsInJson[textId]?.getTextOrNull(locale)!!
                    locale == Locale.ZH_HANT -> localeToText[locale.toString()] = DEFAULT_TRANSLATOR.translate(
                        i18nTexts.format,
                        i18nTexts.getTextOrNull(Locale.ZH_HANS)!!,
                        Locale.ZH_HANS, Locale.ZH_HANT
                    )
                    else -> localeToText[locale.toString()] = DEFAULT_TRANSLATOR.translate(
                        i18nTexts.format,
                        i18nTexts.getTextOrNull(Locale.EN)!!,
                        Locale.EN, locale
                    )
                }
            }
            LocalizedText(textId, localeToText, i18nTexts.format)
        }
}

fun generate(gameDataDir: File, outputI18nDir: File, outputAllJson: File) {
    val i18nResources: List<I18nResource> = gameDataDir.listFiles()
        .filter { it.isDirectory }
        .map { I18nResource(it.name, it.resolve("i18n.yml")) }
        .toMutableList()
        .apply {
            add(I18nResource("common", gameDataDir.resolve("i18n-common.yml")))
        }

    val idToTextAllMap: MutableMap<String, LocalizedText> = mutableMapOf()
    val uniqueIdChecker = mutableSetOf<String>()
    i18nResources.forEach { i18ResourceOfOneMap ->
        val textsOnOneMap: List<LocalizedText> = i18ResourceOfOneMap.generate()
        // check id uniqueness
        textsOnOneMap.forEach {
            require(uniqueIdChecker.add(it.id)) { "Duplicate i18n text: ${it.id}" }
            idToTextAllMap[it.id] = it
        }

        prettyObjectMapper.writeValue(i18ResourceOfOneMap.autoJsonFile, textsOnOneMap)

        val outputDir = outputI18nDir.resolve(i18ResourceOfOneMap.mapId).apply { mkdirs() }
        Locale.values().forEach { locale ->
            val outputJson = outputDir.resolve("${locale.toLowerCase()}.json")
            val outputData = textsOnOneMap.map { it.id to it.getTextOrDefaultLocale(locale) }.toMap()

            uglyObjectMapper.writeValue(outputJson, outputData)
        }
    }
    uglyObjectMapper.writeValue(outputAllJson, idToTextAllMap)
}

private fun File.yamlToLocalizedTexts(): LinkedHashMap<String, LocalizedText> {
    val content = readText()
    return if (content.isEmpty()) LinkedHashMap()
    else YAML_PARSER.readValue(this, object : TypeReference<List<LocalizedText>>() {})
        .associateBy { it.id } as LinkedHashMap<String, LocalizedText>
}

private fun File.jsonToLocalizedTexts(): LinkedHashMap<String, LocalizedText> {
    val content = readText()
    return if (content.isEmpty()) LinkedHashMap()
    else uglyObjectMapper.readValue(this, object : TypeReference<List<LocalizedText>>() {})
        .associateBy { it.id } as LinkedHashMap<String, LocalizedText>
}

val YAML_FACTORY = YAMLFactory()
val YAML_PARSER = ObjectMapper(YAML_FACTORY).registerModule(KotlinModule())
