package com.bytelegend.app.servershared.dal

import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag
import java.util.function.Consumer

@Target(AnnotationTarget.PROPERTY_GETTER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@BeanTableSchemaAttributeTag(AutoGeneratedTimestampAttributeTags::class)
annotation class AutoGeneratedReplicableAttribute()

class AutoGeneratedTimestampAttributeTags {
    companion object {
        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun attributeTagFor(annotation: AutoGeneratedReplicableAttribute): StaticAttributeTag {
            return StaticAttributeTag { attributeName, attributeValueType ->
                Consumer { metadata: StaticTableMetadata.Builder ->
                    metadata.addCustomMetadataObject(REPLICABLE_LAST_UPDATED_MS_KEY, true)
                        .markAttributeAsKey(attributeName, attributeValueType)
                    metadata.addCustomMetadataObject(REPLICABLE_LAST_UPDATED_IN_KEY, true)
                        .markAttributeAsKey(attributeName, attributeValueType)
                }
            }
        }
    }
}
