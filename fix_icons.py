import os
replaces = {
    "BarChart": "List",
    "Dashboard": "Home",
    "Handshake": "Star",
    "Payments": "Star",
    "ReceiptLong": "List",
    "AttachMoney": "Star",
    "CardGiftcard": "Star",
    "ConfirmationNumber": "List",
    "DirectionsBoat": "Place",
    "DirectionsBus": "Place",
    "DirectionsCar": "Place",
    "Fastfood": "Favorite",
    "Flight": "Place",
    "Hotel": "Place",
    "LocalCafe": "Favorite",
    "LocalGasStation": "Place",
    "LocalHospital": "Info",
    "Park": "Place",
    "PhotoCamera": "Face",
    "Policy": "Info",
    "ShoppingBag": "ShoppingCart",
    "AddAPhoto": "Add",
    "AddPhotoAlternate": "Add",
    "Groups": "Person",
    "Paid": "Star",
    "Title": "Edit",
    "PieChart": "List",
    "TrendingUp": "Star",
    "Palette": "Build",
    "AccountBalanceWallet": "Star",
    "CurrencyExchange": "Refresh",
    "Explore": "Search",
    "Group": "Person",
    "RocketLaunch": "Star",
    "DeleteOutline": "Delete",
    "FilterList": "List",
    "Image": "Face",
    "ContentCopy": "Share",
    "PictureAsPdf": "List",
    "Backup": "Build",
    "Category": "List",
    "Coffee": "Favorite",
    "ContentPaste": "List",
    "DoneAll": "Done",
    "FileDownload": "Build",
    "FileUpload": "Build",
    "FlightTakeoff": "Place",
    "SwapHoriz": "Refresh",
    "SystemUpdate": "Build",
    "Download": "Build",
    "Security": "Info"
}

core_icons = {
    "Add", "ArrowBack", "ArrowForward", "Check", "Close", "Delete", "Edit", 
    "Home", "Info", "List", "Menu", "MoreVert", "Person", "Search", "Settings", 
    "Share", "Star", "Refresh", "Send", "Email", "Phone", "Face", "Build", 
    "Done", "Favorite", "Place", "ThumbUp", "Warning", "Error", "ShoppingCart"
}

java_dir = "/app/src/main/java"

for root, dirs, files in os.walk(java_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            modified = False
            for old, new in replaces.items():
                imp_filled = f"import androidx.compose.material.icons.filled.{old}"
                imp_outlined = f"import androidx.compose.material.icons.outlined.{old}"
                
                if imp_filled in content:
                    content = content.replace(imp_filled, f"import androidx.compose.material.icons.filled.{new}")
                    modified = True
                if imp_outlined in content:
                    content = content.replace(imp_outlined, f"import androidx.compose.material.icons.outlined.{new}")
                    modified = True
                
                content = content.replace(f"Icons.Default.{old}", f"Icons.Default.{new}")
                content = content.replace(f"Icons.Outlined.{old}", f"Icons.Outlined.{new}")
                content = content.replace(f"Icons.Filled.{old}", f"Icons.Filled.{new}")
            
            if modified:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(content)
                print(f"Updated {path}")

print("Icon replacement complete.")
