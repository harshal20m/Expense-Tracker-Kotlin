




package com.example.paisatracker.data

data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val country: String,
    val flag: String
)

object CurrencyList {
    val currencies = listOf(
        Currency("INR", "₹", "Indian Rupee", "India", "🇮🇳"),
        Currency("USD", "$", "US Dollar", "United States", "🇺🇸"),
        Currency("EUR", "€", "Euro", "European Union", "🇪🇺"),
        Currency("GBP", "£", "British Pound", "United Kingdom", "🇬🇧"),
        Currency("JPY", "¥", "Japanese Yen", "Japan", "🇯🇵"),
        Currency("CNY", "¥", "Chinese Yuan", "China", "🇨🇳"),
        Currency("AUD", "A$", "Australian Dollar", "Australia", "🇦🇺"),
        Currency("CAD", "C$", "Canadian Dollar", "Canada", "🇨🇦"),
        Currency("CHF", "Fr", "Swiss Franc", "Switzerland", "🇨🇭"),
        Currency("SEK", "kr", "Swedish Krona", "Sweden", "🇸🇪"),
        Currency("NZD", "NZ$", "New Zealand Dollar", "New Zealand", "🇳🇿"),
        Currency("SGD", "S$", "Singapore Dollar", "Singapore", "🇸🇬"),
        Currency("HKD", "HK$", "Hong Kong Dollar", "Hong Kong", "🇭🇰"),
        Currency("NOK", "kr", "Norwegian Krone", "Norway", "🇳🇴"),
        Currency("KRW", "₩", "South Korean Won", "South Korea", "🇰🇷"),
        Currency("TRY", "₺", "Turkish Lira", "Turkey", "🇹🇷"),
        Currency("RUB", "₽", "Russian Ruble", "Russia", "🇷🇺"),
        Currency("BRL", "R$", "Brazilian Real", "Brazil", "🇧🇷"),
        Currency("ZAR", "R", "South African Rand", "South Africa", "🇿🇦"),
        Currency("MXN", "Mex$", "Mexican Peso", "Mexico", "🇲🇽"),
        Currency("AED", "د.إ", "UAE Dirham", "United Arab Emirates", "🇦🇪"),
        Currency("SAR", "﷼", "Saudi Riyal", "Saudi Arabia", "🇸🇦"),
        Currency("THB", "฿", "Thai Baht", "Thailand", "🇹🇭"),
        Currency("MYR", "RM", "Malaysian Ringgit", "Malaysia", "🇲🇾"),
        Currency("IDR", "Rp", "Indonesian Rupiah", "Indonesia", "🇮🇩"),
        Currency("PHP", "₱", "Philippine Peso", "Philippines", "🇵🇭"),
        Currency("VND", "₫", "Vietnamese Dong", "Vietnam", "🇻🇳"),
        Currency("PLN", "zł", "Polish Zloty", "Poland", "🇵🇱"),
        Currency("DKK", "kr", "Danish Krone", "Denmark", "🇩🇰"),
        Currency("CZK", "Kč", "Czech Koruna", "Czech Republic", "🇨🇿"),
        Currency("ILS", "₪", "Israeli Shekel", "Israel", "🇮🇱"),
        Currency("PKR", "₨", "Pakistani Rupee", "Pakistan", "🇵🇰"),
        Currency("BDT", "৳", "Bangladeshi Taka", "Bangladesh", "🇧🇩"),
        Currency("LKR", "Rs", "Sri Lankan Rupee", "Sri Lanka", "🇱🇰"),
        Currency("NPR", "रू", "Nepalese Rupee", "Nepal", "🇳🇵"),
        Currency("EGP", "E£", "Egyptian Pound", "Egypt", "🇪🇬"),
        Currency("NGN", "₦", "Nigerian Naira", "Nigeria", "🇳🇬"),
        Currency("KES", "KSh", "Kenyan Shilling", "Kenya", "🇰🇪"),
        Currency("ARS", "$", "Argentine Peso", "Argentina", "🇦🇷"),
        Currency("CLP", "$", "Chilean Peso", "Chile", "🇨🇱"),
        Currency("COP", "$", "Colombian Peso", "Colombia", "🇨🇴"),
        Currency("PEN", "S/", "Peruvian Sol", "Peru", "🇵🇪")
    )

    fun getCurrencyByCode(code: String): Currency {
        return currencies.find { it.code == code } ?: currencies.first()
    }
}