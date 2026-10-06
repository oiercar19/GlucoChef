package es.racionest1d

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import es.racionest1d.data.*
import es.racionest1d.domain.CarbCalculator
import es.racionest1d.domain.Nutrition
import es.racionest1d.domain.decimal
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.text.DateFormat
import java.util.Date

@Composable internal fun MealEditor(vm: AppViewModel, nav: NavHostController, repeatId: Long, updated: Boolean) {
    val ingredients by vm.ingredients.collectAsState(initial = emptyList())
    val recipes by vm.recipes.collectAsState(initial = emptyList())
    val recipeSummaries by vm.recipeSummaries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val lines = remember(repeatId, updated) { mutableStateListOf<DraftMealItem>() }
    var title by remember(repeatId, updated) { mutableStateOf("") }
    var notes by remember(repeatId, updated) { mutableStateOf("") }
    var picker by remember { mutableStateOf("") }
    var createMode by remember { mutableStateOf("") }
    var editRecipeId by remember { mutableLongStateOf(0L) }
    var ingredientEditorId by remember { mutableLongStateOf(0L) }
    var chosenIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var chosenRecipe by remember { mutableStateOf<Recipe?>(null) }
    var amount by remember { mutableStateOf("1") }
    var mode by remember { mutableStateOf("PORTIONS") }
    var moreOptions by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repeatId, updated) { if (repeatId > 0) {
        vm.getMeal(repeatId)?.let { title = it.meal.title; notes = it.meal.notes }
        lines.clear(); lines.addAll(vm.repeatMeal(repeatId, updated))
    } }
    BackHandler(enabled = createMode.isNotEmpty()) {
        if (createMode != "INGREDIENT" || ingredientEditorId == 0L) picker = createMode
        createMode = ""
        ingredientEditorId = 0L
        editRecipeId = 0L
    }
    if (createMode == "INGREDIENT") {
        IngredientEditor(vm, nav, ingredientEditorId,
            onSaved = { newId ->
                val wasNew = ingredientEditorId == 0L
                chosenIngredient = vm.getIngredient(newId)
                if (wasNew) amount = "100"
                createMode = ""
                ingredientEditorId = 0L
            },
            onCancel = {
                if (ingredientEditorId == 0L) picker = "INGREDIENT"
                createMode = ""
                ingredientEditorId = 0L
            })
        return
    }
    if (createMode == "RECIPE") {
        RecipeEditor(vm, nav, editRecipeId,
            onSaved = { newId ->
                chosenRecipe = vm.getRecipe(newId)?.recipe
                amount = "1"
                mode = "PORTIONS"
                createMode = ""
                editRecipeId = 0L
            },
            onCancel = { picker = "RECIPE"; createMode = ""; editRecipeId = 0L })
        return
    }
    val total = CarbCalculator.sum(lines.map { it.carbs?.let(::Nutrition) })
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (repeatId > 0) "Repetir comida" else "Nueva comida", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (repeatId > 0) Text(if (updated) "Valores actuales: revisa las diferencias señaladas." else "Valores originales guardados en el historial.",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            Text("Añade lo que vas a comer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEachIndexed { index, line ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (line.kind == "RECIPE") recipes.firstOrNull { it.id == line.sourceId }?.photoUri?.let { uri ->
                            AsyncImage(model = uri, contentDescription = "Foto de ${line.name}",
                                modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop)
                        }
                        Text(line.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        NumberInput(line.amount, { newAmount ->
                                lines[index] = line.copy(amount = newAmount)
                                scope.launch {
                                    val quantity = decimal(newAmount)
                                    if (quantity != null && quantity >= BigDecimal.ZERO) {
                                        val n = if (line.usingSavedSnapshot) {
                                            val old = decimal(line.amount)
                                            if (old != null && old > BigDecimal.ZERO && line.carbs != null)
                                                line.carbs.multiply(quantity).divide(old, MathContext.DECIMAL128) else null
                                        } else line.sourceId?.let { id -> runCatching { vm.mealLine(line.kind, id, line.mode, newAmount, updated).carbs }.getOrNull() }
                                        if (index < lines.size && lines[index].name == line.name && lines[index].amount == newAmount)
                                            lines[index] = lines[index].copy(carbs = n)
                                    }
                                }
                            }, "Cantidad en ${line.unit}", Modifier.fillMaxWidth())
                        Text("${pretty(line.carbs)} g de hidratos · ${pretty(line.carbs?.divide(BigDecimal.TEN, MathContext.DECIMAL128))} raciones HC",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        if (line.originalCarbs != null && line.carbs != null && line.originalCarbs.compareTo(line.carbs) != 0)
                            Text("Antes: ${pretty(line.originalCarbs)} g HC · diferencia ${pretty(line.carbs?.subtract(line.originalCarbs))} g",
                                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { lines.removeAt(index) }) { Text("Quitar") }
                        }
                    }
                }
            }
            PrimaryAction("+ Añadir alimento", onClick = { picker = "INGREDIENT" })
            SecondaryAction("+ Añadir plato guardado", onClick = { picker = "RECIPE" })
            TextButton(onClick = { moreOptions = !moreOptions }) {
                Text(if (moreOptions) "Ocultar opciones" else "Nombre, notas y plantilla (opcional)", style = MaterialTheme.typography.bodyLarge)
            }
            if (moreOptions) {
                OutlinedTextField(title, { title = it }, label = { Text("Nombre de la comida") },
                    textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }
            ErrorText(error)
            PrimaryAction("Guardar comida", onClick = { scope.launch { runCatching {
                require(lines.isNotEmpty()) { "Añade un plato o ingrediente" }
                require(lines.all { (decimal(it.amount) ?: BigDecimal(-1)) > BigDecimal.ZERO }) { "Revisa las cantidades" }
                vm.saveMeal(title, notes, false, lines.toList())
            }.onSuccess { nav.navigate("history") { popUpTo("home") } }.onFailure { error = it.message } } })
            if (moreOptions) SecondaryAction("Guardar como plantilla", onClick = { scope.launch { runCatching {
                require(lines.isNotEmpty()) { "Añade un plato o ingrediente" }
                require(lines.all { (decimal(it.amount) ?: BigDecimal(-1)) > BigDecimal.ZERO }) { "Revisa las cantidades" }
                vm.saveMeal(title, notes, true, lines.toList())
            }.onSuccess { nav.navigate("history") { popUpTo("home") } }.onFailure { error = it.message } } })
        }
        Surface(shadowElevation = 8.dp) { Box(Modifier.fillMaxWidth().padding(12.dp)) { Totals(total, "Total de la comida") } }
    }
    if (picker == "INGREDIENT") IngredientPicker(ingredients,
        onDismiss = { picker = "" },
        onCreate = { picker = ""; ingredientEditorId = 0L; createMode = "INGREDIENT" },
        onPick = { chosenIngredient = it; amount = "100"; picker = "" })
    if (picker == "RECIPE") {
        var query by remember { mutableStateOf("") }
        DialogSurface({ picker = "" }) {
            Text("Añadir plato", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(query, { query = it }, label = { Text("Buscar plato") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            SecondaryAction("No está? Crear plato", onClick = { picker = ""; editRecipeId = 0L; createMode = "RECIPE" })
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                val matching = recipeSummaries.filter { it.recipe.name.contains(query, true) }
                if (matching.isEmpty()) Text("No se encontró. Puedes crearlo ahora.", style = MaterialTheme.typography.bodyLarge)
                matching.forEach { summary ->
                    val r = summary.recipe
                    ListItem(headlineContent = { Text(r.name) }, supportingContent = { Text(
                        if (summary.requiresReview) "Revisar valores anteriores antes de añadir"
                        else "${r.portions} porciones${r.finishedWeight?.let { " · $it g finales" } ?: ""}"
                    ) },
                        leadingContent = { r.photoUri?.let { uri ->
                            AsyncImage(model = uri, contentDescription = "Foto de ${r.name}",
                                modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop)
                        } },
                        modifier = Modifier.clickable {
                            if (summary.requiresReview) {
                                editRecipeId = r.id
                                picker = ""
                                createMode = "RECIPE"
                            } else {
                                chosenRecipe = r; amount = "1"; mode = "PORTIONS"; picker = ""
                            }
                        })
                    HorizontalDivider()
                }
            }
            TextButton(onClick = { picker = "" }) { Text("Cancelar") }
        }
    }
    if (chosenIngredient != null || chosenRecipe != null) AlertDialog(onDismissRequest = { chosenIngredient = null; chosenRecipe = null },
        title = { Text(chosenIngredient?.name ?: chosenRecipe?.name.orEmpty()) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Cuánto vas a comer?", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            chosenRecipe?.let { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "PORTIONS", onClick = { mode = "PORTIONS"; amount = "1" }, label = { Text("Porciones", style = MaterialTheme.typography.bodyMedium) })
                    if (r.finishedWeight != null) FilterChip(selected = mode == "GRAMS", onClick = { mode = "GRAMS"; amount = "100" }, label = { Text("Peso final", style = MaterialTheme.typography.bodyMedium) })
                }
                if (mode == "PORTIONS") Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("0,5", "1", "2").forEach { v -> FilterChip(selected = amount == v,
                        onClick = { amount = v }, label = { Text(v, style = MaterialTheme.typography.bodyLarge) }) }
                }
            }
            NumberInput(amount, { amount = it }, chosenIngredient?.let { "Cantidad en ${it.unit}" }
                ?: if (mode == "GRAMS") "Gramos del plato preparado" else "Porciones del plato", Modifier.fillMaxWidth())
            chosenIngredient?.let { ingredient ->
                SecondaryAction(if (ingredient.custom) "Editar valor del alimento" else "Usar otro valor", onClick = {
                    ingredientEditorId = ingredient.id
                    createMode = "INGREDIENT"
                })
            }
        } }, confirmButton = { TextButton(onClick = { scope.launch { runCatching {
            require((decimal(amount) ?: BigDecimal.ZERO) > BigDecimal.ZERO) { "Cantidad inválida" }
            val item = chosenIngredient?.let { vm.mealLine("INGREDIENT", it.id, "AMOUNT", amount) }
                ?: chosenRecipe?.let { vm.mealLine("RECIPE", it.id, mode, amount) }
            if (item != null) lines.add(item)
        }.onSuccess { chosenIngredient = null; chosenRecipe = null }.onFailure { error = it.message } } }) { Text("Añadir") } },
        dismissButton = { TextButton(onClick = { chosenIngredient = null; chosenRecipe = null }) { Text("Cancelar") } })
}

@Composable internal fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val meals by vm.meals.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    Page("Historial", "Aquí están tus comidas y plantillas guardadas.") {
        meals.forEach { record ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("${if (record.meal.isTemplate) "Plantilla · " else ""}${record.meal.title}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.meal.occurredAt)),
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${pretty(record.meal.carbsSnapshot?.let(::decimal))} g de hidratos · ${pretty(record.meal.carbsSnapshot?.let(::decimal)?.divide(BigDecimal.TEN))} raciones HC",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    PrimaryAction("Repetir comida", onClick = { nav.navigate("meal/${record.meal.id}/false") })
                    TextButton(onClick = { expandedId = if (expandedId == record.meal.id) null else record.meal.id }) {
                        Text(if (expandedId == record.meal.id) "Ocultar detalles" else "Ver detalles y opciones", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (expandedId == record.meal.id) {
                        record.items.forEach { item -> Text("${item.nameSnapshot}: ${item.amount} ${item.unit} · ${pretty(item.carbsSnapshot?.let(::decimal))} g HC",
                            style = MaterialTheme.typography.bodyMedium) }
                        if (record.meal.notes.isNotBlank()) Text(record.meal.notes, style = MaterialTheme.typography.bodyMedium)
                        SecondaryAction("Repetir con valores actuales", onClick = { nav.navigate("meal/${record.meal.id}/true") })
                        TextButton(onClick = { deleteId = record.meal.id }) { Text("Eliminar registro") }
                    }
                }
            }
        }
        if (meals.isEmpty()) Text("Todavía no hay comidas guardadas. Empieza desde Inicio.", style = MaterialTheme.typography.bodyLarge)
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("¿Eliminar registro?") },
        text = { Text("Esta acción borra la comida o plantilla guardada.") },
        confirmButton = { TextButton(onClick = { scope.launch { vm.deleteMeal(id) }; deleteId = null }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } }) }
}

@Composable internal fun SettingsScreen(vm: AppViewModel) {
    val dark by vm.darkTheme.collectAsState(initial = false)
    val largeText by vm.largeText.collectAsState(initial = false)
    Page("Ajustes", "Los datos se guardan solo en este dispositivo.") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("Texto más grande", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = largeText, onCheckedChange = vm::setLargeText)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("Tema oscuro", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = dark, onCheckedChange = vm::setDarkTheme)
        }
    }
}

