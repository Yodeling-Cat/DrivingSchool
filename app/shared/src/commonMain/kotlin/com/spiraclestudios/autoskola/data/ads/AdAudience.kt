package com.spiraclestudios.autoskola.data.ads

/**
 * Who an ad request is for, as far as what they may be shown goes. The SDK needs it before any
 * request, and only the app knows it, so [AdRepository] derives it from the age answer and pushes it.
 *
 * Separate from whether the ads are personalized ([AdDataSource.setPersonalizedAds]): that is the
 * user's own answer, while this is a fact about their age that no answer changes.
 */
enum class AdAudience {
    /**
     * Under the age of consent, or not yet said. Google is told the user is under the age of consent,
     * which makes its ads non-personalized, and the content rating is capped at general audiences.
     * Also what an unanswered question means: the flag comes off on an answer the user gave, never by
     * default.
     */
    Child,

    /**
     * 16 or 17. Not under the age of consent, so none of the child treatment — its general-audiences
     * cap would give a 17-year-old learner children's content — but still a minor: the content rating
     * is capped at teen, and personalization is not theirs to turn on.
     */
    Teen,

    /** 18 or older. No cap, and personalization follows their own answer. */
    Adult,
}
