package com.example.export

import com.example.data.entity.CategoryEntity
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus

object CsvBackupManager {

    fun exportBundlesToCsv(bundles: List<TripBackupBundle>): String {
        val sb = StringBuilder()
        sb.append("# APP: Trip Tracker\n")
        sb.append("# VERSION: 2\n")
        sb.append("# EXPORTED_AT: ${System.currentTimeMillis()}\n\n")

        // 1. TRIPS
        sb.append("# SECTION: TRIPS\n")
        sb.append("id,name,destination,startDate,endDate,budget,currencySymbol,baseCurrency,status,managerName,tripCode,isReadOnly\n")
        bundles.forEachIndexed { index, b ->
            val t = b.trip
            sb.append("${index},\"${escape(t.name)}\",\"${escape(t.destination)}\",${t.startDate},${t.endDate},${t.budget},\"${escape(t.currencySymbol)}\",\"${escape(t.baseCurrency)}\",${t.status.name},\"${escape(t.managerName)}\",\"${escape(t.tripCode)}\",${t.isReadOnly}\n")
        }
        sb.append("\n")

        // 2. MEMBERS
        sb.append("# SECTION: MEMBERS\n")
        sb.append("tripIndex,originalId,name,phoneNumber,colorHex,avatarInitials,isManager\n")
        bundles.forEachIndexed { tripIdx, b ->
            b.members.forEach { m ->
                sb.append("${tripIdx},${m.id},\"${escape(m.name)}\",\"${escape(m.phoneNumber)}\",\"${escape(m.colorHex)}\",\"${escape(m.avatarInitials)}\",${m.isManager}\n")
            }
        }
        sb.append("\n")

        // 3. DEPOSITS
        sb.append("# SECTION: DEPOSITS\n")
        sb.append("tripIndex,memberId,contributorName,amount,categoryName,date,notes\n")
        bundles.forEachIndexed { tripIdx, b ->
            b.deposits.forEach { d ->
                sb.append("${tripIdx},${d.memberId ?: -1},\"${escape(d.contributorName)}\",${d.amount},\"${escape(d.categoryName)}\",${d.date},\"${escape(d.notes)}\"\n")
            }
        }
        sb.append("\n")

        // 4. EXPENSES
        sb.append("# SECTION: EXPENSES\n")
        sb.append("tripIndex,originalId,title,amount,category,customCategoryName,isIncome,date,payerType,payerMemberId,splitMode,notes\n")
        bundles.forEachIndexed { tripIdx, b ->
            b.expenses.forEach { e ->
                sb.append("${tripIdx},${e.id},\"${escape(e.title)}\",${e.amount},${e.category.name},\"${escape(e.customCategoryName ?: "")}\",${e.isIncome},${e.date},${e.payerType.name},${e.payerMemberId ?: -1},${e.splitMode.name},\"${escape(e.notes)}\"\n")
            }
        }
        sb.append("\n")

        // 5. CATEGORIES
        sb.append("# SECTION: CATEGORIES\n")
        sb.append("tripIndex,name,iconName,colorHex,isIncome\n")
        bundles.forEachIndexed { tripIdx, b ->
            b.categories.forEach { c ->
                sb.append("${tripIdx},\"${escape(c.name)}\",\"${escape(c.iconName)}\",\"${escape(c.colorHex)}\",${c.isIncome}\n")
            }
        }
        sb.append("\n")

        // 6. SPLITS
        sb.append("# SECTION: SPLITS\n")
        sb.append("expenseId,memberId,assignedAmount\n")
        bundles.forEachIndexed { _, b ->
            b.splits.forEach { s ->
                sb.append("${s.expenseId},${s.memberId},${s.assignedAmount}\n")
            }
        }
        sb.append("\n")

        // 7. PAYERS
        sb.append("# SECTION: PAYERS\n")
        sb.append("expenseId,memberId,amountPaid\n")
        bundles.forEachIndexed { _, b ->
            b.payers.forEach { p ->
                sb.append("${p.expenseId},${p.memberId},${p.amountPaid}\n")
            }
        }

        return sb.toString()
    }

    private fun escape(s: String): String = s.replace("\"", "\"\"")

    fun parseBundlesFromCsv(csvString: String): List<TripBackupBundle> {
        val lines = csvString.lines()
        val tripRows = mutableListOf<Array<String>>()
        val memberRows = mutableListOf<Array<String>>()
        val depositRows = mutableListOf<Array<String>>()
        val expenseRows = mutableListOf<Array<String>>()
        val categoryRows = mutableListOf<Array<String>>()
        val splitRows = mutableListOf<Array<String>>()
        val payerRows = mutableListOf<Array<String>>()

        var currentSection = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# SECTION:")) {
                currentSection = trimmed.removePrefix("# SECTION:").trim()
                continue
            }
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue
            if (trimmed.startsWith("id,name") || trimmed.startsWith("tripIndex,originalId") ||
                trimmed.startsWith("tripIndex,memberId") || trimmed.startsWith("tripIndex,title") ||
                trimmed.startsWith("tripIndex,name") || trimmed.startsWith("expenseId,memberId,assignedAmount") ||
                trimmed.startsWith("expenseId,memberId,amountPaid")) {
                continue
            }

            val parsedFields = parseCsvLine(trimmed)
            when (currentSection) {
                "TRIPS" -> tripRows.add(parsedFields)
                "MEMBERS" -> memberRows.add(parsedFields)
                "DEPOSITS" -> depositRows.add(parsedFields)
                "EXPENSES" -> expenseRows.add(parsedFields)
                "CATEGORIES" -> categoryRows.add(parsedFields)
                "SPLITS" -> splitRows.add(parsedFields)
                "PAYERS" -> payerRows.add(parsedFields)
            }
        }

        val tripBundles = mutableMapOf<Int, TripBackupBundle>()

        tripRows.forEachIndexed { index, fields ->
            if (fields.size >= 10) {
                val name = fields.getOrNull(1) ?: "Trip"
                val destination = fields.getOrNull(2) ?: ""
                val startDate = fields.getOrNull(3)?.toLongOrNull() ?: System.currentTimeMillis()
                val endDate = fields.getOrNull(4)?.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L)
                val budget = fields.getOrNull(5)?.toDoubleOrNull() ?: 50000.0
                val currencySymbol = fields.getOrNull(6) ?: "৳"
                val baseCurrency = fields.getOrNull(7) ?: "BDT"
                val statusStr = fields.getOrNull(8) ?: "ACTIVE"
                val managerName = fields.getOrNull(9) ?: "Manager"
                val tripCode = fields.getOrNull(10) ?: ""
                val isReadOnly = fields.getOrNull(11)?.toBoolean() ?: false

                val trip = TripEntity(
                    id = 0,
                    name = name,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    budget = budget,
                    currencySymbol = currencySymbol,
                    baseCurrency = baseCurrency,
                    status = runCatching { TripStatus.valueOf(statusStr) }.getOrDefault(TripStatus.ACTIVE),
                    managerName = managerName,
                    tripCode = tripCode,
                    isReadOnly = isReadOnly
                )
                tripBundles[index] = TripBackupBundle(
                    trip = trip,
                    members = emptyList(),
                    deposits = emptyList(),
                    expenses = emptyList(),
                    splits = emptyList(),
                    payers = emptyList(),
                    categories = emptyList()
                )
            }
        }

        val tripMembersMap = mutableMapOf<Int, MutableList<MemberEntity>>()
        memberRows.forEach { fields ->
            val tripIdx = fields.getOrNull(0)?.toIntOrNull() ?: 0
            val oldId = fields.getOrNull(1)?.toLongOrNull() ?: 0L
            val name = fields.getOrNull(2) ?: "Member"
            val phone = fields.getOrNull(3) ?: ""
            val color = fields.getOrNull(4) ?: "#006C4C"
            val initials = fields.getOrNull(5) ?: "MB"
            val isManager = fields.getOrNull(6)?.toBoolean() ?: false

            val member = MemberEntity(
                id = oldId,
                tripId = 0,
                name = name,
                phoneNumber = phone,
                colorHex = color,
                avatarInitials = initials,
                isManager = isManager
            )
            tripMembersMap.getOrPut(tripIdx) { mutableListOf() }.add(member)
        }

        val tripDepositsMap = mutableMapOf<Int, MutableList<DepositEntity>>()
        depositRows.forEach { fields ->
            val tripIdx = fields.getOrNull(0)?.toIntOrNull() ?: 0
            val memberId = fields.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }
            val contributor = fields.getOrNull(2) ?: ""
            val amount = fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val category = fields.getOrNull(4) ?: "Group Fund Deposit"
            val date = fields.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()
            val notes = fields.getOrNull(6) ?: ""

            val deposit = DepositEntity(
                tripId = 0,
                memberId = memberId,
                contributorName = contributor,
                amount = amount,
                categoryName = category,
                date = date,
                notes = notes
            )
            tripDepositsMap.getOrPut(tripIdx) { mutableListOf() }.add(deposit)
        }

        val tripCategoriesMap = mutableMapOf<Int, MutableList<CategoryEntity>>()
        categoryRows.forEach { fields ->
            val tripIdx = fields.getOrNull(0)?.toIntOrNull() ?: 0
            val name = fields.getOrNull(1) ?: "Category"
            val icon = fields.getOrNull(2) ?: "ticket"
            val color = fields.getOrNull(3) ?: "#006C4C"
            val isIncome = fields.getOrNull(4)?.toBoolean() ?: false

            val cat = CategoryEntity(
                tripId = 0,
                name = name,
                iconName = icon,
                colorHex = color,
                isIncome = isIncome
            )
            tripCategoriesMap.getOrPut(tripIdx) { mutableListOf() }.add(cat)
        }

        val tripExpensesMap = mutableMapOf<Int, MutableList<ExpenseEntity>>()
        expenseRows.forEach { fields ->
            val tripIdx = fields.getOrNull(0)?.toIntOrNull() ?: 0
            val oldExpId = fields.getOrNull(1)?.toLongOrNull() ?: 0L
            val title = fields.getOrNull(2) ?: "Expense"
            val amount = fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val catStr = fields.getOrNull(4) ?: "FOOD"
            val customCat = fields.getOrNull(5)?.ifBlank { null }
            val isIncome = fields.getOrNull(6)?.toBoolean() ?: false
            val date = fields.getOrNull(7)?.toLongOrNull() ?: System.currentTimeMillis()
            val payerTypeStr = fields.getOrNull(8) ?: "COMMON_FUND"
            val payerMemberId = fields.getOrNull(9)?.toLongOrNull()?.takeIf { it > 0 }
            val splitModeStr = fields.getOrNull(10) ?: "EQUAL"
            val notes = fields.getOrNull(11) ?: ""

            val expense = ExpenseEntity(
                id = oldExpId,
                tripId = 0,
                title = title,
                amount = amount,
                category = runCatching { ExpenseCategory.valueOf(catStr) }.getOrDefault(ExpenseCategory.FOOD),
                customCategoryName = customCat,
                isIncome = isIncome,
                date = date,
                payerType = runCatching { PayerType.valueOf(payerTypeStr) }.getOrDefault(PayerType.COMMON_FUND),
                payerMemberId = payerMemberId,
                splitMode = runCatching { SplitMode.valueOf(splitModeStr) }.getOrDefault(SplitMode.EQUAL),
                notes = notes
            )
            tripExpensesMap.getOrPut(tripIdx) { mutableListOf() }.add(expense)
        }

        val allSplits = mutableListOf<ExpenseSplitEntity>()
        splitRows.forEach { fields ->
            val expId = fields.getOrNull(0)?.toLongOrNull() ?: 0L
            val memberId = fields.getOrNull(1)?.toLongOrNull() ?: 0L
            val assigned = fields.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            allSplits.add(ExpenseSplitEntity(expenseId = expId, memberId = memberId, assignedAmount = assigned))
        }

        val allPayers = mutableListOf<ExpensePayerEntity>()
        payerRows.forEach { fields ->
            val expId = fields.getOrNull(0)?.toLongOrNull() ?: 0L
            val memberId = fields.getOrNull(1)?.toLongOrNull() ?: 0L
            val amountPaid = fields.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            allPayers.add(ExpensePayerEntity(expenseId = expId, memberId = memberId, amountPaid = amountPaid))
        }

        return tripBundles.map { (tripIdx, bundle) ->
            bundle.copy(
                members = tripMembersMap[tripIdx] ?: emptyList(),
                deposits = tripDepositsMap[tripIdx] ?: emptyList(),
                categories = tripCategoriesMap[tripIdx] ?: emptyList(),
                expenses = tripExpensesMap[tripIdx] ?: emptyList(),
                splits = allSplits,
                payers = allPayers
            )
        }
    }

    private fun parseCsvLine(line: String): Array<String> {
        val list = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    list.add(sb.toString())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        list.add(sb.toString())
        return list.toTypedArray()
    }
}
