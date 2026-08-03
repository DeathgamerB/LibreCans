import re

with open('./app/src/main/kotlin/org/librecans/app/SettingsControl.kt', 'r') as f:
    content = f.read()

# Import LocalScrollValue and LocalDensity
imports = [
    "import org.librecans.app.LocalScrollValue",
    "import androidx.compose.ui.platform.LocalDensity"
]
for imp in imports:
    if imp not in content:
        content = content.replace("import androidx.compose.ui.unit.sp", f"import androidx.compose.ui.unit.sp\n{imp}")

# Inject scroll reading logic into DeviceHeroCard
device_hero_card_start = r"fun DeviceHeroCard\(\s*deviceName: String,\s*deviceMac: String,\s*batteryLevel: Int,\s*batteryCharging: Boolean,\s*batteryLeft: Int,\s*batteryRight: Int,\s*batteryCase: Int\s*\)\s*\{"

device_hero_card_replacement = """fun DeviceHeroCard(
    deviceName: String,
    deviceMac: String,
    batteryLevel: Int,
    batteryCharging: Boolean,
    batteryLeft: Int,
    batteryRight: Int,
    batteryCase: Int
) {
    val scrollValue = LocalScrollValue.current
    val dynamicScale = (1f - (scrollValue / with(LocalDensity.current) { 400.dp.toPx() })).coerceIn(0.5f, 1f)"""

content = re.sub(device_hero_card_start, device_hero_card_replacement, content)

# Modify the image
image_regex = r"Image\(\s*painter = painterResource\(id = if \(isTWS\) R\.drawable\.ic_device_earbuds else R\.drawable\.ic_device_headphones\),\s*contentDescription = if \(isTWS\) \"Earbuds\" else \"Headphones\",\s*modifier = Modifier\s*\.fillMaxWidth\(0\.8f\)\s*\.aspectRatio\(1f\),\s*contentScale = ContentScale\.Fit\s*\)"

new_image = """Image(
                    painter = painterResource(id = if (isTWS) R.drawable.ic_device_earbuds else R.drawable.ic_device_headphones),
                    contentDescription = if (isTWS) "Earbuds" else "Headphones",
                    modifier = Modifier
                        .fillMaxWidth(dynamicScale)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )"""

content = re.sub(image_regex, new_image, content)

with open('./app/src/main/kotlin/org/librecans/app/SettingsControl.kt', 'w') as f:
    f.write(content)
