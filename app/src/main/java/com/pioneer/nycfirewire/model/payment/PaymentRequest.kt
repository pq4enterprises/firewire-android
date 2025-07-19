package com.pioneer.nycfirewire.model.payment

data class PaymentRequest(
    var userId: String="",
    var paymentMethod: String="",
    var paymentToken: String="",
    var transactionId: String="",
    var amount: String="",
    var currency: String="",
    var status: String="",
    var purchaseDate: String="",
    var expiredDate: String="",
    var type: String=""
)
