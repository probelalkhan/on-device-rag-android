package dev.belalkhan.semanticsearch.model

data class SearchDocument(val id: String, val text: String)

val searchDocuments = listOf(
    SearchDocument("01", "Cakes"),
    SearchDocument("02", "Dogs"),
    SearchDocument("03", "Cars"),
    SearchDocument("04", "Sofas"),
    SearchDocument("05", "Shoes"),
    SearchDocument("06", "Roses"),
    SearchDocument("07", "Guitars"),
    SearchDocument("08", "Airplanes"),
    SearchDocument("09", "Pencils"),
    SearchDocument("10", "Milk"),
    SearchDocument("11", "Beds"),
    SearchDocument("12", "Jackets"),
    SearchDocument("13", "Football"),
    SearchDocument("14", "Clocks"),
    SearchDocument("15", "Bicycles")
)
