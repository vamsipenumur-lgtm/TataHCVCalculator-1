package com.tatamotors.hcvcalculator.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tatamotors.hcvcalculator.logic.BrtLogic
import org.json.JSONArray
import org.json.JSONObject

fun String.toD(default: Double = 0.0): Double = this.toDoubleOrNull() ?: default

/** Customer name, location and route of operation — common to all three calculators. */
class CustomerInfo {
    var name by mutableStateOf("")
    var location by mutableStateOf("")
    var route by mutableStateOf("")

    fun toJson() = JSONObject().put("name", name).put("location", location).put("route", route)
    fun fromJson(o: JSONObject) {
        name = o.optString("name"); location = o.optString("location"); route = o.optString("route")
    }
}

/** FE MaX calculator state. */
class FeState {
    val customer = CustomerInfo()
    var vehicle by mutableStateOf("55T / 6.7L MAV")
    var monthlyKm by mutableStateOf("10000")
    var mileage by mutableStateOf("3.1")
    var diesel by mutableStateOf("100")
    var years by mutableStateOf("5")
    var selectedPct by mutableStateOf(7)

    fun toJson(): JSONObject = JSONObject()
        .put("customer", customer.toJson())
        .put("vehicle", vehicle).put("monthlyKm", monthlyKm).put("mileage", mileage)
        .put("diesel", diesel).put("years", years).put("selectedPct", selectedPct)

    fun fromJson(s: String) {
        try {
            val o = JSONObject(s)
            o.optJSONObject("customer")?.let { customer.fromJson(it) }
            vehicle = o.optString("vehicle", vehicle)
            monthlyKm = o.optString("monthlyKm", monthlyKm)
            mileage = o.optString("mileage", mileage)
            diesel = o.optString("diesel", diesel)
            years = o.optString("years", years)
            selectedPct = o.optInt("selectedPct", selectedPct)
        } catch (_: Exception) { }
    }
}

/** Revenue MaX (High Payload Truck) calculator state. */
class HptState {
    val customer = CustomerInfo()
    var tonnageIdx by mutableStateOf(0)
    var addlPayload by mutableStateOf("1.8")
    var loadedPct by mutableStateOf("100")
    var yearlyKm by mutableStateOf("65000")
    var rate by mutableStateOf("2.5")
    var years by mutableStateOf("5")

    fun toJson(): JSONObject = JSONObject()
        .put("customer", customer.toJson())
        .put("tonnageIdx", tonnageIdx).put("addlPayload", addlPayload)
        .put("loadedPct", loadedPct).put("yearlyKm", yearlyKm)
        .put("rate", rate).put("years", years)

    fun fromJson(s: String) {
        try {
            val o = JSONObject(s)
            o.optJSONObject("customer")?.let { customer.fromJson(it) }
            tonnageIdx = o.optInt("tonnageIdx", tonnageIdx)
            addlPayload = o.optString("addlPayload", addlPayload)
            loadedPct = o.optString("loadedPct", loadedPct)
            yearlyKm = o.optString("yearlyKm", yearlyKm)
            rate = o.optString("rate", rate)
            years = o.optString("years", years)
        } catch (_: Exception) { }
    }
}

/** One tyre group's editable state (BRT). */
class TyreGroupState(label: String, count: String, cost: String, life: String) {
    var label by mutableStateOf(label)
    var count by mutableStateOf(count)
    var cost by mutableStateOf(cost)
    var life by mutableStateOf(life)
    fun toModel() = BrtLogic.TyreGroup(label, count.toD().toInt(), cost.toD(), life.toD())
    fun toJson() = JSONObject().put("label", label).put("count", count).put("cost", cost).put("life", life)
}

/** All editable inputs for one BRT product. */
class ProductState(defaultName: String) {
    var name by mutableStateOf(defaultName)
    var vehiclePrice by mutableStateOf("2821000")
    var bodyPrice by mutableStateOf("350000")
    var fundingPct by mutableStateOf("100")
    var interestPct by mutableStateOf("11")
    var tenureYears by mutableStateOf("5")
    var moratorium by mutableStateOf("1")
    var payload by mutableStateOf("18")
    var primaryLoad by mutableStateOf("18")
    var returnLoad by mutableStateOf("18")
    var primaryLead by mutableStateOf("982")
    var returnLead by mutableStateOf("982")
    var emptyKm by mutableStateOf("0")
    var tripsPerMonth by mutableStateOf("6")
    var operativeMonths by mutableStateOf("12")
    var primaryRate by mutableStateOf("3.6")
    var secondaryRate by mutableStateOf("3.6")
    var mileage by mutableStateOf("4.5")
    var fuelPrice by mutableStateOf("90")
    var defPct by mutableStateOf("6")
    var defCost by mutableStateOf("60")
    var maintPerKm by mutableStateOf("0.9")
    var tollPerKm by mutableStateOf("5.5")
    var addlPerTon by mutableStateOf("0")
    var crewSalary by mutableStateOf("70000")
    var adminPerYear by mutableStateOf("25000")
    var insurancePct by mutableStateOf("4")
    val tyreGroups = mutableStateListOf(
        TyreGroupState("Front axle", "2", "25000", "100000"),
        TyreGroupState("Drive/Rear axles", "8", "25000", "100000")
    )

    fun toModel() = BrtLogic.ProductInputs(
        name = name,
        vehiclePrice = vehiclePrice.toD(), bodyPrice = bodyPrice.toD(),
        fundingPct = fundingPct.toD(), interestPctPa = interestPct.toD(),
        tenureYears = tenureYears.toD(), moratoriumMonths = moratorium.toD(),
        payloadTons = payload.toD(),
        primaryLoadTons = primaryLoad.toD(), returnLoadTons = returnLoad.toD(),
        primaryLeadKm = primaryLead.toD(), returnLeadKm = returnLead.toD(), emptyKm = emptyKm.toD(),
        tripsPerMonth = tripsPerMonth.toD(), operativeMonths = operativeMonths.toD(),
        primaryRatePerTonKm = primaryRate.toD(), secondaryRatePerTonKm = secondaryRate.toD(),
        mileageKmpl = mileage.toD(), fuelPricePerLitre = fuelPrice.toD(),
        defConsumptionPct = defPct.toD(), defCostPerLitre = defCost.toD(),
        maintenancePerKm = maintPerKm.toD(), tollPerKm = tollPerKm.toD(),
        addlExpensePerTon = addlPerTon.toD(),
        crewSalaryPerMonth = crewSalary.toD(), adminExpensesPerYear = adminPerYear.toD(),
        insurancePctOfCost = insurancePct.toD(),
        tyreGroups = tyreGroups.map { it.toModel() }
    )

    fun toJson(): JSONObject {
        val o = JSONObject()
            .put("name", name).put("vehiclePrice", vehiclePrice).put("bodyPrice", bodyPrice)
            .put("fundingPct", fundingPct).put("interestPct", interestPct)
            .put("tenureYears", tenureYears).put("moratorium", moratorium)
            .put("payload", payload).put("primaryLoad", primaryLoad).put("returnLoad", returnLoad)
            .put("primaryLead", primaryLead).put("returnLead", returnLead).put("emptyKm", emptyKm)
            .put("tripsPerMonth", tripsPerMonth).put("operativeMonths", operativeMonths)
            .put("primaryRate", primaryRate).put("secondaryRate", secondaryRate)
            .put("mileage", mileage).put("fuelPrice", fuelPrice)
            .put("defPct", defPct).put("defCost", defCost)
            .put("maintPerKm", maintPerKm).put("tollPerKm", tollPerKm).put("addlPerTon", addlPerTon)
            .put("crewSalary", crewSalary).put("adminPerYear", adminPerYear).put("insurancePct", insurancePct)
        val arr = JSONArray()
        tyreGroups.forEach { arr.put(it.toJson()) }
        o.put("tyreGroups", arr)
        return o
    }

    fun fromJson(o: JSONObject) {
        name = o.optString("name", name)
        vehiclePrice = o.optString("vehiclePrice", vehiclePrice); bodyPrice = o.optString("bodyPrice", bodyPrice)
        fundingPct = o.optString("fundingPct", fundingPct); interestPct = o.optString("interestPct", interestPct)
        tenureYears = o.optString("tenureYears", tenureYears); moratorium = o.optString("moratorium", moratorium)
        payload = o.optString("payload", payload)
        primaryLoad = o.optString("primaryLoad", primaryLoad); returnLoad = o.optString("returnLoad", returnLoad)
        primaryLead = o.optString("primaryLead", primaryLead); returnLead = o.optString("returnLead", returnLead)
        emptyKm = o.optString("emptyKm", emptyKm)
        tripsPerMonth = o.optString("tripsPerMonth", tripsPerMonth)
        operativeMonths = o.optString("operativeMonths", operativeMonths)
        primaryRate = o.optString("primaryRate", primaryRate); secondaryRate = o.optString("secondaryRate", secondaryRate)
        mileage = o.optString("mileage", mileage); fuelPrice = o.optString("fuelPrice", fuelPrice)
        defPct = o.optString("defPct", defPct); defCost = o.optString("defCost", defCost)
        maintPerKm = o.optString("maintPerKm", maintPerKm); tollPerKm = o.optString("tollPerKm", tollPerKm)
        addlPerTon = o.optString("addlPerTon", addlPerTon)
        crewSalary = o.optString("crewSalary", crewSalary); adminPerYear = o.optString("adminPerYear", adminPerYear)
        insurancePct = o.optString("insurancePct", insurancePct)
        o.optJSONArray("tyreGroups")?.let { arr ->
            tyreGroups.clear()
            for (i in 0 until arr.length()) {
                val g = arr.optJSONObject(i) ?: continue
                tyreGroups.add(
                    TyreGroupState(
                        g.optString("label"), g.optString("count"),
                        g.optString("cost"), g.optString("life")
                    )
                )
            }
            if (tyreGroups.isEmpty())
                tyreGroups.add(TyreGroupState("Front axle", "2", "25000", "100000"))
        }
    }
}

/** Full BRT comparison state: customer + two products + horizon. */
class BrtState {
    val customer = CustomerInfo()
    val product1 = ProductState("Tata Signa 2823.T")
    val product2 = ProductState("Competition Model")
    var compareYears by mutableStateOf("5")

    fun toJson(): JSONObject = JSONObject()
        .put("customer", customer.toJson())
        .put("product1", product1.toJson())
        .put("product2", product2.toJson())
        .put("compareYears", compareYears)

    fun fromJson(s: String) {
        try {
            val o = JSONObject(s)
            o.optJSONObject("customer")?.let { customer.fromJson(it) }
            o.optJSONObject("product1")?.let { product1.fromJson(it) }
            o.optJSONObject("product2")?.let { product2.fromJson(it) }
            compareYears = o.optString("compareYears", compareYears)
        } catch (_: Exception) { }
    }
}
