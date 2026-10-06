package es.racionest1d

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import es.racionest1d.data.*
import es.racionest1d.domain.CarbCalculator
import es.racionest1d.domain.decimal
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.io.File
import java.util.UUID

@Composable internal fun DialogSurface(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable internal fun IngredientPicker(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onCreate: (() -> Unit)? = null,
    onPick: (Ingredient) -> Unit
) {
    var query by remember { mutableStateOf("") }
    DialogSurface(onDismiss) {
        Text("Elegir alimento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(query, { query = it }, label = { Text("Buscar alimento") },
            textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
        if (onCreate != null) SecondaryAction("No está? Crear alimento", onClick = onCreate)
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            val matching = ingredients.filter { it.name.contains(query, true) || it.variant.contains(query, true) }
            if (matching.isEmpty()) Text("No se encontró. Puedes crearlo ahora.", style = MaterialTheme.typography.bodyLarge)
            matching.forEach { i ->
                ListItem(headlineContent = { Text(i.name, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("${i.variant} · ${when (i.dataStatus) {
                        "PENDING" -> "valor pendiente"
                        "ESTIMATED" -> "orientativo"
                        else -> i.state
                    }}", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.clickable { onPick(i) })
                HorizontalDivider()
            }
        }
        TextButton(onClick = onDismiss) { Text("Cancelar") }
    }
}

@Composable internal fun RecipesScreen(vm: AppViewModel, nav: NavHostController) {
    val all by vm.recipeSummaries.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var optionsId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    Page("Mis platos", "Abre un plato guardado o crea uno nuevo.") {
        PrimaryAction("+ Crear plato") { nav.navigate("recipe/0") }
        OutlinedTextField(query, { query = it }, label = { Text("Buscar plato") },
            textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Solo favoritos", style = MaterialTheme.typography.bodyMedium) })
        all.filter { (!favoritesOnly || it.recipe.favorite) &&
            (it.recipe.name.contains(query, true) || it.recipe.category.contains(query, true)) }.forEach { summary ->
            val r = summary.recipe
            val names = summary.ingredientNames.take(4).joinToString(", ")
            val remaining = summary.ingredientNames.size - 4
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.photoUri?.let { uri ->
                        AsyncImage(model = uri, contentDescription = "Foto del plato ${r.name}",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentScale = ContentScale.Crop)
                    }
                    Text("${if (r.favorite) "★ " else ""}${r.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${r.portions} ${if (r.portions == "1") "porción" else "porciones"}", style = MaterialTheme.typography.bodyLarge)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                        Text(summary.perPortion?.let { "Por porción: ${pretty(it.portions)} raciones de HC" }
                            ?: "Raciones de HC por porción: pendientes",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (decimal(r.portions)?.compareTo(BigDecimal.ONE) != 0)
                        Text(summary.total?.let { "Plato completo: ${pretty(it.portions)} raciones de HC" }
                            ?: "Raciones de HC del plato completo: pendientes",
                            style = MaterialTheme.typography.bodyMedium)
                    Text("Ingredientes: $names${if (remaining > 0) " y $remaining más" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (summary.requiresReview) Text(
                        "Este plato usa valores anteriores. Abre el plato y cambia esos alimentos antes de volver a usarlo.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error
                    )
                    SecondaryAction("Abrir plato", onClick = { nav.navigate("recipe/${r.id}") })
                    TextButton(onClick = { optionsId = if (optionsId == r.id) null else r.id }) { Text("Más opciones") }
                    if (optionsId == r.id) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { scope.launch { vm.duplicateRecipe(r.id) } }) { Text("Duplicar") }
                            TextButton(onClick = { deleteId = r.id }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null },
        title = { Text("¿Eliminar plato?") }, text = { Text("Las comidas guardadas conservarán su copia histórica.") },
        confirmButton = { TextButton(onClick = { scope.launch { vm.deleteRecipe(id) }; deleteId = null }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } }) }
}

@Composable internal fun RecipeEditor(
    vm: AppViewModel,
    nav: NavHostController,
    id: Long,
    onSaved: suspend (Long) -> Unit = { nav.popBackStack() },
    onCancel: () -> Unit = { nav.popBackStack() }
) {
    val all by vm.ingredients.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val draft = remember(id) { mutableStateListOf<DraftIngredient>() }
    var name by remember(id) { mutableStateOf("") }
    var category by remember(id) { mutableStateOf("Comida") }
    var portions by remember(id) { mutableStateOf("1") }
    var finalWeight by remember(id) { mutableStateOf("") }
    var notes by remember(id) { mutableStateOf("") }
    var photoUri by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var pendingPhotoUri by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var photoEdited by rememberSaveable(id) { mutableStateOf(false) }
    var favorite by remember(id) { mutableStateOf(false) }
    var createdAt by remember(id) { mutableLongStateOf(System.currentTimeMillis()) }
    var showPicker by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Ingredient?>(null) }
    var pickedAmount by remember { mutableStateOf("100") }
    var replacing by remember { mutableIntStateOf(-1) }
    var createIngredient by remember { mutableStateOf(false) }
    var ingredientEditorId by remember { mutableLongStateOf(0L) }
    var moreOptions by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { photoUri = pendingPhotoUri; photoEdited = true }
        pendingPhotoUri = null
    }
    LaunchedEffect(id) { if (id > 0) vm.getRecipe(id)?.let { loaded ->
        name = loaded.recipe.name; category = loaded.recipe.category; portions = loaded.recipe.portions
        finalWeight = loaded.recipe.finishedWeight.orEmpty(); notes = loaded.recipe.notes
        if (!photoEdited) photoUri = loaded.recipe.photoUri
        favorite = loaded.recipe.favorite; createdAt = loaded.recipe.createdAt
        draft.clear()
        draft.addAll(loaded.ingredients.map { line ->
            val ingredient = line.ingredientId?.let { vm.getIngredient(it) } ?: Ingredient(
                name = line.nameSnapshot, category = "Otros", variant = line.variantSnapshot, state = "",
                weightBasis = "", unit = line.unit, gramsPerPortion = line.gramsPerPortionSnapshot,
                carbsPerHundred = line.carbsPerHundredSnapshot, dataStatus = line.statusSnapshot,
                source = line.sourceSnapshot, sourceDate = "")
            DraftIngredient(ingredient, line.amount)
        })
    } }
    BackHandler(enabled = createIngredient) {
        if (ingredientEditorId == 0L) showPicker = true
        createIngredient = false
        ingredientEditorId = 0L
    }
    if (createIngredient) {
        IngredientEditor(vm, nav, ingredientEditorId,
            onSaved = { newId ->
                val wasNew = ingredientEditorId == 0L
                picked = vm.getIngredient(newId)
                if (wasNew) pickedAmount = "100"
                createIngredient = false
                ingredientEditorId = 0L
            },
            onCancel = {
                if (ingredientEditorId == 0L) showPicker = true
                createIngredient = false
                ingredientEditorId = 0L
            })
        return
    }
    val total = CarbCalculator.sum(draft.map { runCatching { vm.nutrition(it.ingredient, it.amount) }.getOrNull() })
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (id == 0L) "Crear plato" else "Editar plato", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("1. Ponle nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre del plato") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
            Text("Foto del plato (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            photoUri?.let { uri ->
                AsyncImage(model = uri, contentDescription = "Foto de ${name.ifBlank { "este plato" }}",
                    modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
            }
            SecondaryAction(if (photoUri == null) "Hacer foto" else "Cambiar foto", onClick = {
                runCatching {
                    val directory = File(context.filesDir, "recipe_photos").apply { mkdirs() }
                    val file = File(directory, "${UUID.randomUUID()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingPhotoUri = uri.toString()
                    takePhoto.launch(uri)
                }.onFailure {
                    pendingPhotoUri = null
                    error = "No se pudo abrir la cámara: ${it.message ?: "comprueba si hay una app de cámara"}"
                }
            })
            if (photoUri != null) TextButton(onClick = { photoUri = null; photoEdited = true }) { Text("Quitar foto") }
            Text("2. Añade los alimentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            draft.forEachIndexed { index, line ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${line.ingredient.name} · ${line.ingredient.variant}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (line.ingredient.id == 0L) Text(
                            "Valor anterior: cambia este alimento por uno del catálogo nuevo.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error
                        )
                        NumberInput(line.amount, { draft[index] = line.copy(amount = it) }, "Cantidad en ${line.ingredient.unit}", Modifier.fillMaxWidth())
                        val n = runCatching { vm.nutrition(line.ingredient, line.amount) }.getOrNull()
                        Text("${pretty(n?.carbs)} g de hidratos · ${pretty(n?.portions)} raciones HC",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { replacing = index; showPicker = true }) { Text("Cambiar alimento") }
                            TextButton(onClick = { draft.removeAt(index) }) { Text("Quitar") }
                        }
                    }
                }
            }
            PrimaryAction("+ Añadir alimento", onClick = { replacing = -1; showPicker = true })
            Text("3. ¿Cuántas porciones salen?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Una porción es la parte del plato que comes.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1", "2", "4").forEach { count -> FilterChip(selected = portions == count,
                    onClick = { portions = count }, label = { Text(count, style = MaterialTheme.typography.bodyLarge) }) }
            }
            NumberInput(portions, { portions = it }, "Número de porciones", Modifier.fillMaxWidth())
            TextButton(onClick = { moreOptions = !moreOptions }) {
                Text(if (moreOptions) "Ocultar opciones" else "Más opciones (opcional)", style = MaterialTheme.typography.bodyLarge)
            }
            if (moreOptions) {
                NumberInput(finalWeight, { finalWeight = it }, "Peso final del plato (g)", Modifier.fillMaxWidth())
                Text("Solo hace falta si quieres calcular lo que comes por gramos del plato ya preparado.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(favorite, { favorite = it }); Text("Favorito", style = MaterialTheme.typography.bodyLarge) }
                Text("Si desechas líquido o cambia la receta, revisa el cálculo.", style = MaterialTheme.typography.bodyMedium)
            }
            ErrorText(error)
            PrimaryAction("Guardar plato", onClick = { scope.launch { runCatching {
                require(name.isNotBlank()) { "Escribe el nombre" }
                require(draft.isNotEmpty()) { "Añade al menos un ingrediente" }
                require(draft.all { it.ingredient.id > 0L }) { "Cambia los alimentos del catálogo anterior antes de guardar" }
                require(draft.all { (decimal(it.amount) ?: BigDecimal.ZERO) > BigDecimal.ZERO }) { "Revisa los pesos" }
                vm.saveRecipe(Recipe(id = id, name = name.trim(), category = category,
                    portions = portions, finishedWeight = finalWeight.takeIf { it.isNotBlank() }, notes = notes,
                    photoUri = photoUri, favorite = favorite, createdAt = createdAt), draft.toList())
            }.onSuccess { onSaved(it) }.onFailure { error = it.message } } })
            SecondaryAction("Volver sin guardar", onClick = onCancel)
        }
        Surface(shadowElevation = 8.dp) { Box(Modifier.fillMaxWidth().padding(12.dp)) { Totals(total, "Total de la receta") } }
    }
    if (showPicker) IngredientPicker(all, onDismiss = { showPicker = false },
        onCreate = { showPicker = false; ingredientEditorId = 0L; createIngredient = true }, onPick = {
        picked = it; pickedAmount = if (replacing >= 0 && replacing < draft.size) draft[replacing].amount else "100"; showPicker = false
    })
    picked?.let { i -> AlertDialog(onDismissRequest = { picked = null }, title = { Text("${i.name} · ${i.variant}") },
        text = { Column {
            Text("${i.state} · ${i.weightBasis}", style = MaterialTheme.typography.bodyMedium)
            NumberInput(pickedAmount, { pickedAmount = it }, "Cantidad en ${i.unit}", Modifier.fillMaxWidth())
            val n = runCatching { vm.nutrition(i, pickedAmount) }.getOrNull()
            Text("${pretty(n?.carbs)} g de hidratos · ${pretty(n?.portions)} raciones HC", style = MaterialTheme.typography.bodyLarge)
            SecondaryAction(if (i.custom) "Editar valor del alimento" else "Usar otro valor", onClick = {
                ingredientEditorId = i.id
                createIngredient = true
            })
        } }, confirmButton = { TextButton(onClick = {
            if ((decimal(pickedAmount) ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                if (replacing >= 0 && replacing < draft.size) draft[replacing] = DraftIngredient(i, pickedAmount)
                else draft.add(DraftIngredient(i, pickedAmount))
                replacing = -1; picked = null
            }
        }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = { picked = null }) { Text("Cancelar") } }) }
}

