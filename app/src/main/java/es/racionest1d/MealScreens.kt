package es.racionest1d

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@Composable internal fun MealEditor(vm: AppViewModel, nav: NavHostController, repeatId: Long, updated: Boolean, editing: Boolean = false) {
    val ingredients by vm.ingredients.collectAsState(initial = emptyList())
    val recipes by vm.recipes.collectAsState(initial = emptyList())
    val recipeSummaries by vm.recipeSummaries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val lines = remember(repeatId, updated, editing) { mutableStateListOf<DraftMealItem>() }
    var title by remember(repeatId, updated, editing) { mutableStateOf("") }
    var notes by remember(repeatId, updated, editing) { mutableStateOf("") }
    var insulin by remember(repeatId, updated, editing) { mutableStateOf("") }
    var manualCarbRations by remember(repeatId, updated, editing) { mutableStateOf("") }
    var picker by remember { mutableStateOf("") }
    var createMode by remember { mutableStateOf("") }
    var editRecipeId by remember { mutableLongStateOf(0L) }
    var ingredientEditorId by remember { mutableLongStateOf(0L) }
    var chosenIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var amount by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repeatId, updated, editing) { if (repeatId > 0) {
        vm.getMeal(repeatId)?.let {
            title = it.meal.title; notes = it.meal.notes; insulin = it.meal.insulin
            manualCarbRations = it.meal.manualCarbRations.orEmpty()
        }
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
                val line = vm.mealLine("RECIPE", newId, "PORTIONS", "1")
                lines.add(line)
                createMode = ""
                editRecipeId = 0L
            },
            onCancel = { picker = "RECIPE"; createMode = ""; editRecipeId = 0L })
        return
    }
    val calculatedTotal = CarbCalculator.sum(lines.map { it.carbs?.let(::Nutrition) })
    val manualRations = decimal(manualCarbRations)?.takeIf { it >= BigDecimal.ZERO }
    val total = manualRations?.let { Nutrition(it.multiply(BigDecimal.TEN)) } ?: calculatedTotal
    val saveMeal: () -> Unit = {
        scope.launch { runCatching {
            require(lines.isNotEmpty()) { "Añade un plato o ingrediente" }
            require(lines.all { (decimal(it.amount) ?: BigDecimal(-1)) > BigDecimal.ZERO }) { "Revisa las cantidades" }
            val manualValue = manualCarbRations.takeIf { it.isNotBlank() }
            if (editing) vm.updateMeal(repeatId, title, notes, insulin, lines.toList(), manualValue)
            else vm.saveMeal(title, notes, false, lines.toList(), insulin, manualValue)
        }.onSuccess {
            if (editing || repeatId > 0) nav.popBackStack()
            else nav.navigate("history") { popUpTo("meal/-1/false") { inclusive = true }; launchSingleTop = true }
        }.onFailure { error = it.message } }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(when { editing -> "Editar comida"; repeatId > 0 -> "Repetir comida"; else -> "Nueva comida" },
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (repeatId > 0 && !editing) Text(if (updated) "Usando los valores actuales de los alimentos." else "Se mantienen los valores guardados.",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            Text("Añade lo que vas a comer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEachIndexed { index, line ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val recipe = if (line.kind == "RECIPE") recipes.firstOrNull { it.id == line.sourceId } else null
                        recipe?.photoUri?.let { uri ->
                            AsyncImage(model = uri, contentDescription = "Foto de ${line.name}",
                                modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop)
                        }
                        Text(line.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val amountLabel = when {
                                line.kind != "RECIPE" -> "Cantidad (${line.unit})"
                                line.mode == "GRAMS" -> "Gramos"
                                else -> "Cantidad del plato"
                            }
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
                                            if (index < lines.size && lines[index].name == line.name && lines[index].amount == newAmount &&
                                                lines[index].rationsInput == line.rationsInput && lines[index].mode == line.mode)
                                                lines[index] = lines[index].copy(carbs = n,
                                                    rationsInput = n?.divide(BigDecimal.TEN, MathContext.DECIMAL128)?.let(::pretty)?.takeUnless { it == "—" }.orEmpty())
                                        }
                                    }
                                }, amountLabel, Modifier.weight(1f))
                            NumberInput(line.rationsInput, { value ->
                                val carbs = decimal(value)?.multiply(BigDecimal.TEN)
                                lines[index] = line.copy(carbs = carbs, rationsInput = value)
                            }, "Raciones HC", Modifier.weight(1f))
                        }
                        Text("${pretty(line.carbs)} g de hidratos · ${pretty(line.carbs?.divide(BigDecimal.TEN, MathContext.DECIMAL128))} raciones HC",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        if (line.originalCarbs != null && line.carbs != null && line.originalCarbs.compareTo(line.carbs) != 0)
                            Text("Antes: ${pretty(line.originalCarbs)} g HC · diferencia ${pretty(line.carbs.subtract(line.originalCarbs))} g",
                                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { lines.removeAt(index) }) { Text("Quitar") }
                        }
                    }
                }
            }
            PrimaryAction("+ Añadir alimento", onClick = { picker = "INGREDIENT" })
            SecondaryAction("+ Añadir plato guardado", onClick = { picker = "RECIPE" })
            OutlinedTextField(title, { title = it }, label = { Text("Nombre de la comida (opcional)") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(insulin, { insulin = it }, label = { Text("Insulina (unidades, opcional)") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            NumberInput(manualCarbRations, { manualCarbRations = it }, "Raciones HC manuales de la comida (opcional)", Modifier.fillMaxWidth())
            Text("Si lo rellenas, sustituye la suma de los alimentos.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(notes, { notes = it }, label = { Text("Observaciones (opcional)") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${pretty(total?.carbs)} g HC · ${pretty(total?.portions)} raciones HC",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                ErrorText(error)
                PrimaryAction(if (editing) "Guardar cambios" else "Guardar comida", onClick = saveMeal)
            }
        }
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
                        else "${pretty(summary.total?.portions)} raciones HC"
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
                                picker = ""
                                scope.launch { runCatching { vm.mealLine("RECIPE", r.id, "PORTIONS", "1") }
                                    .onSuccess { lines.add(it) }.onFailure { error = it.message } }
                            }
                        })
                    HorizontalDivider()
                }
            }
            TextButton(onClick = { picker = "" }) { Text("Cancelar") }
        }
    }
    chosenIngredient?.let { ingredient -> AlertDialog(onDismissRequest = { chosenIngredient = null },
        title = { Text(ingredient.name) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cantidad del alimento", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            NumberInput(amount, { amount = it }, "Cantidad en ${ingredient.unit}", Modifier.fillMaxWidth())
            SecondaryAction(if (ingredient.custom) "Editar valor del alimento" else "Usar otro valor", onClick = {
                ingredientEditorId = ingredient.id
                createMode = "INGREDIENT"
            })
        } }, confirmButton = { TextButton(onClick = { scope.launch { runCatching {
            require((decimal(amount) ?: BigDecimal.ZERO) > BigDecimal.ZERO) { "Cantidad inválida" }
            lines.add(vm.mealLine("INGREDIENT", ingredient.id, "AMOUNT", amount))
        }.onSuccess { chosenIngredient = null }.onFailure { error = it.message } } }) { Text("Añadir") } },
        dismissButton = { TextButton(onClick = { chosenIngredient = null }) { Text("Cancelar") } }) }
}

@Composable internal fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val meals by vm.meals.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var deleteId by remember { mutableStateOf<Long?>(null) }
    Page("Historial", "Aquí están tus comidas y plantillas guardadas.") {
        meals.forEach { record ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${if (record.meal.isTemplate) "Plantilla · " else ""}${record.meal.title}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.meal.occurredAt)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${pretty(record.meal.carbsSnapshot?.let(::decimal))} g de hidratos",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${pretty(record.meal.carbsSnapshot?.let(::decimal)?.divide(BigDecimal.TEN))} raciones HC",
                                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            if (record.meal.insulin.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${record.meal.insulin} U", color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text("Insulina", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        record.items.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.nameSnapshot, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("${item.amount} ${item.unit} · ${pretty(item.carbsSnapshot?.let(::decimal))} g HC · ${pretty(item.carbsSnapshot?.let(::decimal)?.divide(BigDecimal.TEN))} raciones HC",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (record.meal.notes.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Observaciones", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Text(record.meal.notes, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { nav.navigate("meal-edit/${record.meal.id}") }) { Text("Editar") }
                        TextButton(onClick = { nav.navigate("meal/${record.meal.id}/false") }) { Text("Repetir") }
                        TextButton(onClick = { deleteId = record.meal.id }) { Text("Eliminar") }
                    }
                }
            }
        }
        if (meals.isEmpty()) Text("Todavía no hay comidas guardadas. Registra una desde Comida.", style = MaterialTheme.typography.bodyLarge)
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

