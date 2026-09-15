package com.gameball.gameball.inappmessaging.model

/**
 * What a host's beforeDisplay hook decides about a message the SDK has selected.
 *
 * [LATER] defers into the pending slot and the message is retried at the next opportunity.
 * [DISCARD] spends the occurrence: nothing displays and nothing is retried.
 */
enum class DisplayDecision { SHOW, LATER, DISCARD }
