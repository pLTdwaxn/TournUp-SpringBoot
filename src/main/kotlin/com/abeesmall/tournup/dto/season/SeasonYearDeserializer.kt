package com.abeesmall.tournup.dto.season

import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

// Jackson otherwise permits truncating a fractional year to an integer.
class SeasonYearDeserializer : StdDeserializer<Int>(Int::class.javaObjectType) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Int {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return parser.intValue
        }
        return context.handleUnexpectedToken(Int::class.javaObjectType, parser) as Int
    }
}
