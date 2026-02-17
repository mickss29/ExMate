package com.example.exmate;

import java.util.HashMap;

public class CategoryMapper {

    private static final HashMap<String, String> keywordToCategory = new HashMap<>();

    static {
        // FOOD
        keywordToCategory.put("chai", "Food");
        keywordToCategory.put("tea", "Food");
        keywordToCategory.put("coffee", "Food");
        keywordToCategory.put("starbucks", "Food");
        keywordToCategory.put("pizza", "Food");
        keywordToCategory.put("burger", "Food");
        keywordToCategory.put("dominos", "Food");
        keywordToCategory.put("dinner", "Food");
        keywordToCategory.put("lunch", "Food");
        keywordToCategory.put("breakfast", "Food");
        keywordToCategory.put("snacks", "Food");
        keywordToCategory.put("biryani", "Food");

        // TRAVEL
        keywordToCategory.put("uber", "Travel");
        keywordToCategory.put("ola", "Travel");
        keywordToCategory.put("auto", "Travel");
        keywordToCategory.put("cab", "Travel");
        keywordToCategory.put("metro", "Travel");
        keywordToCategory.put("bus", "Travel");
        keywordToCategory.put("train", "Travel");

        // SHOPPING
        keywordToCategory.put("amazon", "Shopping");
        keywordToCategory.put("flipkart", "Shopping");
        keywordToCategory.put("shopping", "Shopping");

        // BILLS
        keywordToCategory.put("recharge", "Bills");
        keywordToCategory.put("wifi", "Bills");
        keywordToCategory.put("electricity", "Bills");
        keywordToCategory.put("bill", "Bills");
        keywordToCategory.put("mobile", "Bills");

        // FITNESS
        keywordToCategory.put("gym", "Fitness");
        keywordToCategory.put("protein", "Fitness");
        keywordToCategory.put("creatine", "Fitness");
        keywordToCategory.put("whey", "Fitness");

        // HEALTH
        keywordToCategory.put("medicine", "Health");
        keywordToCategory.put("hospital", "Health");
        keywordToCategory.put("doctor", "Health");

        // ENTERTAINMENT
        keywordToCategory.put("movie", "Entertainment");
        keywordToCategory.put("pvr", "Entertainment");
        keywordToCategory.put("netflix", "Entertainment");
        keywordToCategory.put("spotify", "Entertainment");
    }

    public static String detectCategory(String noteLowerCase) {
        if (noteLowerCase == null) return "Other";

        for (String key : keywordToCategory.keySet()) {
            if (noteLowerCase.contains(key)) {
                return keywordToCategory.get(key);
            }
        }
        return "Other";
    }
}
