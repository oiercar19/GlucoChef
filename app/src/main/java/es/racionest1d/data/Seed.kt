package es.racionest1d.data

/** Catálogo de prueba facilitado por el usuario: cada peso corresponde a 1 ración (10 g HC). */
object Seed {
    const val version = "Lista personal v1, 2026-10-04"

    private fun item(
        name: String, category: String, grams: Int,
        variant: String = "General", state: String = "Sin especificar"
    ) = Ingredient(
        name = name, category = category, variant = variant, state = state,
        weightBasis = "Peso del alimento", gramsPerPortion = grams.toString(),
        dataStatus = "ESTIMATED",
        source = "Valores facilitados por el usuario", sourceDate = "2026-10-04",
        sourceVersion = version
    )

    val ingredients = listOf(
        // 15 g por 1 ración
        item("Arroz", "Cereales", 15, "Crudo", "Crudo"),
        item("Macarrones", "Cereales", 15, "Crudos", "Crudo"),
        item("Puré de patatas", "Tubérculos", 15),
        item("Cereales", "Cereales", 15),
        item("Harina de trigo", "Cereales", 15),
        item("Crocante", "Dulces y postres", 15),
        item("Galletas", "Pan y galletas", 15),
        item("Cuscús", "Cereales", 15),
        item("Pan tostado", "Pan y galletas", 15),
        item("Palitos de pan", "Pan y galletas", 15),

        // 20 g por 1 ración
        item("Pan blanco", "Pan y galletas", 20),
        item("Garbanzo", "Legumbres", 20, "Crudo", "Crudo"),
        item("Alubia", "Legumbres", 20, "Cruda", "Crudo"),
        item("Lentejas", "Legumbres", 20, "Crudas", "Crudo"),
        item("Palomitas", "Aperitivos", 20),
        item("Patatas fritas", "Tubérculos", 20, "Fritas", "Frito"),
        item("Maíz tostado", "Aperitivos", 20, "Tostado", "Tostado"),

        // 30 y 45 g por 1 ración
        item("Castañas", "Frutos secos", 30),
        item("Arroz", "Cereales", 45, "Cocido", "Cocido"),
        item("Macarrones", "Cereales", 45, "Cocidos", "Cocido"),

        // 50 g por 1 ración
        item("Alubia", "Legumbres", 50, "Cocida", "Cocido"),
        item("Garbanzo", "Legumbres", 50, "Cocido", "Cocido"),
        item("Lentejas", "Legumbres", 50, "Cocidas", "Cocido"),
        item("Patata", "Tubérculos", 50),
        item("Plátano", "Frutas", 50),
        item("Uva", "Frutas", 50),

        // 100 g por 1 ración
        item("Guisantes", "Legumbres", 100),
        item("Habas", "Legumbres", 100),
        item("Soja", "Legumbres", 100, "Cruda", "Crudo"),
        item("Cerezas", "Frutas", 100),
        item("Pera", "Frutas", 100),
        item("Piña", "Frutas", 100),
        item("Higo", "Frutas", 100),
        item("Manzana", "Frutas", 100),
        item("Naranja", "Frutas", 100),
        item("Mandarina", "Frutas", 100),
        item("Melocotón", "Frutas", 100),
        item("Kiwi", "Frutas", 100),
        item("Ciruela", "Frutas", 100),

        // 150 g por 1 ración
        item("Albaricoque", "Frutas", 150),
        item("Cebolla", "Hortalizas", 150),
        item("Zanahoria", "Hortalizas", 150),

        // 200 g por 1 ración
        item("Calabaza", "Hortalizas", 200),
        item("Soja", "Legumbres", 200, "Cocida", "Cocido"),
        item("Fresas", "Frutas", 200),
        item("Melón", "Frutas", 200),
        item("Pomelo", "Frutas", 200),
        item("Sandía", "Frutas", 200),

        // 250 g por 1 ración. La lista original habla de peso, también para leche.
        item("Vainas", "Hortalizas", 250),
        item("Yogur", "Lácteos", 250),
        item("Queso", "Lácteos", 250),
        item("Leche", "Lácteos", 250),

        // 300 g por 1 ración
        item("Pimiento rojo", "Hortalizas", 300),
        item("Pimiento verde", "Hortalizas", 300),
        item("Tomate", "Hortalizas", 300),
        item("Puerro", "Hortalizas", 300),
        item("Berza", "Hortalizas", 300),
        item("Alcachofa", "Hortalizas", 300),
        item("Calabacín", "Hortalizas", 300),
        item("Pepino", "Hortalizas", 300),
        item("Brócoli", "Hortalizas", 300),
        item("Coliflor", "Hortalizas", 300),
        item("Acelga", "Hortalizas", 300),
        item("Espárragos", "Hortalizas", 300),
        item("Endivias", "Hortalizas", 300),
        item("Champiñones", "Hortalizas", 300)
    )
}
