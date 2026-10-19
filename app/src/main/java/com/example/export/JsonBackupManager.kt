package com.example.export

import com.example.data.entity.CategoryEntity
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.PayerType
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.SplitMode
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import org.json.JSONArray
import org.json.JSONObject

data class TripBackupBundle(
    val trip: TripEntity,
    val members: List<MemberEntity>,
    val deposits: List<DepositEntity>,
    val expenses: List<ExpenseEntity>,
    val splits: List<ExpenseSplitEntity>,
    val payers: List<ExpensePayerEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val settlements: List<SettlementRecordEntity> = emptyList()
)

object JsonBackupManager {

    fun exportTripToJson(bundle: TripBackupBundle): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "TripSplit BD")
        root.put("exportedAt", System.currentTimeMillis())

        // Trip
        val tObj = JSONObject().apply {
            put("name", bundle.trip.name)
            put("destination", bundle.trip.destination)
            put("startDate", bundle.trip.startDate)
            put("endDate", bundle.trip.endDate)
            put("budget", bundle.trip.budget)
            put("currencySymbol", bundle.trip.currencySymbol)
            put("baseCurrency", bundle.trip.baseCurrency)
            put("status", bundle.trip.status.name)
            put("managerName", bundle.trip.managerName)
        }
        root.put("trip", tObj)

        // Members (keep a mapping from old ID to export index so we can remap on import)
        val mArray = JSONArray()
        bundle.members.forEachIndexed { index, m ->
            mArray.put(JSONObject().apply {
                put("index", index)
                put("originalId", m.id)
                put("name", m.name)
                put("phoneNumber", m.phoneNumber)
                put("colorHex", m.colorHex)
                put("avatarInitials", m.avatarInitials)
                put("isManager", m.isManager)
            })
        }
        root.put("members", mArray)

        // Deposits
        val dArray = JSONArray()
        bundle.deposits.forEach { d ->
            dArray.put(JSONObject().apply {
                put("memberOriginalId", d.memberId ?: -1L)
                put("contributorName", d.contributorName)
                put("amount", d.amount)
                put("categoryName", d.categoryName)
                put("date", d.date)
                put("notes", d.notes)
            })
        }
        root.put("deposits", dArray)

        // Expenses
        val eArray = JSONArray()
        bundle.expenses.forEachIndexed { expIdx, exp ->
            val expObj = JSONObject().apply {
                put("expIndex", expIdx)
                put("originalId", exp.id)
                put("title", exp.title)
                put("amount", exp.amount)
                put("category", exp.category.name)
                put("customCategoryName", exp.customCategoryName ?: "")
                put("customCategoryIcon", exp.customCategoryIcon ?: "")
                put("customCategoryColor", exp.customCategoryColor ?: "")
                put("isIncome", exp.isIncome)
                put("date", exp.date)
                put("payerType", exp.payerType.name)
                put("payerMemberOriginalId", exp.payerMemberId ?: -1L)
                put("splitMode", exp.splitMode.name)
                put("notes", exp.notes)
            }

            // Splits for this expense
            val splitsForExp = bundle.splits.filter { it.expenseId == exp.id }
            val splitsArr = JSONArray()
            splitsForExp.forEach { s ->
                splitsArr.put(JSONObject().apply {
                    put("memberOriginalId", s.memberId)
                    put("assignedAmount", s.assignedAmount)
                })
            }
            expObj.put("splits", splitsArr)

            // Payers for this expense
            val payersForExp = bundle.payers.filter { it.expenseId == exp.id }
            val payersArr = JSONArray()
            payersForExp.forEach { p ->
                payersArr.put(JSONObject().apply {
                    put("memberOriginalId", p.memberId)
                    put("amountPaid", p.amountPaid)
                })
            }
            expObj.put("payers", payersArr)

            eArray.put(expObj)
        }
        root.put("expenses", eArray)

        // Categories
        val cArray = JSONArray()
        bundle.categories.forEach { c ->
            cArray.put(JSONObject().apply {
                put("name", c.name)
                put("iconName", c.iconName)
                put("colorHex", c.colorHex)
                put("isIncome", c.isIncome)
            })
        }
        root.put("categories", cArray)

        return root.toString(2)
    }

    fun parseTripFromJson(jsonString: String): TripBackupBundle? {
        return runCatching {
            val root = JSONObject(jsonString)
            val tObj = root.getJSONObject("trip")
            val trip = TripEntity(
                name = tObj.optString("name", "Imported Trip"),
                destination = tObj.optString("destination", "Bangladesh"),
                startDate = tObj.optLong("startDate", System.currentTimeMillis()),
                endDate = tObj.optLong("endDate", System.currentTimeMillis() + 86400000L),
                budget = tObj.optDouble("budget", 50000.0),
                currencySymbol = "৳",
                baseCurrency = "BDT",
                status = runCatching { TripStatus.valueOf(tObj.optString("status", "ACTIVE")) }.getOrDefault(TripStatus.ACTIVE),
                managerName = tObj.optString("managerName", "Tour Manager")
            )

            // Members
            val mArray = root.optJSONArray("members") ?: JSONArray()
            val members = mutableListOf<MemberEntity>()

            for (i in 0 until mArray.length()) {
                val mObj = mArray.getJSONObject(i)
                val oldId = mObj.optLong("originalId", (i + 1).toLong())
                val member = MemberEntity(
                    id = oldId,
                    tripId = 0,
                    name = mObj.optString("name", "Member ${i + 1}"),
                    phoneNumber = mObj.optString("phoneNumber", ""),
                    colorHex = mObj.optString("colorHex", "#006C4C"),
                    avatarInitials = mObj.optString("avatarInitials", "MB"),
                    isManager = mObj.optBoolean("isManager", i == 0)
                )
                members.add(member)
            }

            // Deposits
            val dArray = root.optJSONArray("deposits") ?: JSONArray()
            val deposits = mutableListOf<DepositEntity>()
            for (i in 0 until dArray.length()) {
                val dObj = dArray.getJSONObject(i)
                val memberId = dObj.optLong("memberOriginalId", -1L).takeIf { it > 0 }
                deposits.add(
                    DepositEntity(
                        tripId = 0,
                        memberId = memberId,
                        contributorName = dObj.optString("contributorName", ""),
                        amount = dObj.optDouble("amount", 0.0),
                        categoryName = dObj.optString("categoryName", "Group Fund Deposit"),
                        date = dObj.optLong("date", System.currentTimeMillis()),
                        notes = dObj.optString("notes", "")
                    )
                )
            }

            // Expenses, splits, payers
            val eArray = root.optJSONArray("expenses") ?: JSONArray()
            val expenses = mutableListOf<ExpenseEntity>()
            val splits = mutableListOf<ExpenseSplitEntity>()
            val payers = mutableListOf<ExpensePayerEntity>()

            for (i in 0 until eArray.length()) {
                val eObj = eArray.getJSONObject(i)
                val oldExpId = eObj.optLong("originalId", (i + 1).toLong())
                val payerMemberId = eObj.optLong("payerMemberOriginalId", -1L).takeIf { it > 0 }

                val expense = ExpenseEntity(
                    id = oldExpId,
                    tripId = 0,
                    title = eObj.optString("title", "Expense"),
                    amount = eObj.optDouble("amount", 0.0),
                    category = runCatching { ExpenseCategory.valueOf(eObj.optString("category", "FOOD")) }.getOrDefault(ExpenseCategory.FOOD),
                    customCategoryName = eObj.optString("customCategoryName").ifBlank { null },
                    customCategoryIcon = eObj.optString("customCategoryIcon").ifBlank { null },
                    customCategoryColor = eObj.optString("customCategoryColor").ifBlank { null },
                    isIncome = eObj.optBoolean("isIncome", false),
                    date = eObj.optLong("date", System.currentTimeMillis()),
                    payerType = runCatching { PayerType.valueOf(eObj.optString("payerType", "COMMON_FUND")) }.getOrDefault(PayerType.COMMON_FUND),
                    payerMemberId = payerMemberId,
                    splitMode = runCatching { SplitMode.valueOf(eObj.optString("splitMode", "EQUAL")) }.getOrDefault(SplitMode.EQUAL),
                    notes = eObj.optString("notes", "")
                )
                expenses.add(expense)

                // Splits
                val sArr = eObj.optJSONArray("splits") ?: JSONArray()
                for (sIdx in 0 until sArr.length()) {
                    val sObj = sArr.getJSONObject(sIdx)
                    splits.add(
                        ExpenseSplitEntity(
                            expenseId = oldExpId,
                            memberId = sObj.optLong("memberOriginalId"),
                            assignedAmount = sObj.optDouble("assignedAmount", 0.0)
                        )
                    )
                }

                // Payers
                val pArr = eObj.optJSONArray("payers") ?: JSONArray()
                for (pIdx in 0 until pArr.length()) {
                    val pObj = pArr.getJSONObject(pIdx)
                    payers.add(
                        ExpensePayerEntity(
                            expenseId = oldExpId,
                            memberId = pObj.optLong("memberOriginalId"),
                            amountPaid = pObj.optDouble("amountPaid", 0.0)
                        )
                    )
                }
            }

            // Categories
            val cArray = root.optJSONArray("categories") ?: JSONArray()
            val categories = mutableListOf<CategoryEntity>()
            for (i in 0 until cArray.length()) {
                val cObj = cArray.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        tripId = 0,
                        name = cObj.optString("name", "Custom"),
                        iconName = cObj.optString("iconName", "ticket"),
                        colorHex = cObj.optString("colorHex", "#006C4C"),
                        isIncome = cObj.optBoolean("isIncome", false)
                    )
                )
            }

            TripBackupBundle(
                trip = trip,
                members = members,
                deposits = deposits,
                expenses = expenses,
                splits = splits,
                payers = payers,
                categories = categories
            )
        }.getOrNull()
    }
}
