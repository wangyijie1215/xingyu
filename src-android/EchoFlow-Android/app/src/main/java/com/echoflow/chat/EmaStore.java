package com.echoflow.chat;

/* loaded from: classes.dex */
public class EmaStore {

    /* loaded from: classes.dex */
    public static class Ema {
        public java.lang.String author = "user";
        public java.lang.String cardId;
        public long createdAt;
        public java.lang.String id;
        public boolean mentioned;
        public java.lang.String text;

        public org.json.JSONObject toJson() throws java.lang.Exception {
            org.json.JSONObject jSONObject = new org.json.JSONObject();
            jSONObject.put("id", this.id);
            java.lang.String str = this.cardId;
            if (str == null) {
                str = "";
            }
            jSONObject.put("cardId", str);
            java.lang.String str2 = this.text;
            jSONObject.put("text", str2 != null ? str2 : "");
            java.lang.String str3 = this.author;
            if (str3 == null) {
                str3 = "user";
            }
            jSONObject.put("author", str3);
            jSONObject.put("createdAt", this.createdAt);
            jSONObject.put("mentioned", this.mentioned);
            return jSONObject;
        }

        public static com.echoflow.chat.EmaStore.Ema fromJson(org.json.JSONObject jSONObject) {
            com.echoflow.chat.EmaStore.Ema ema = new com.echoflow.chat.EmaStore.Ema();
            ema.id = jSONObject.optString("id", "");
            ema.cardId = jSONObject.optString("cardId", "");
            ema.text = jSONObject.optString("text", "");
            ema.author = jSONObject.optString("author", "user");
            ema.createdAt = jSONObject.optLong("createdAt", 0L);
            ema.mentioned = jSONObject.optBoolean("mentioned", false);
            return ema;
        }
    }

    private static java.io.File emaFile(android.content.Context context, java.lang.String str) {
        java.io.File file = new java.io.File(context.getFilesDir(), "ema");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new java.io.File(file, str + ".json");
    }

    public static java.util.List<com.echoflow.chat.EmaStore.Ema> list(android.content.Context context, java.lang.String str) {
        int i;
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.io.File emaFile = emaFile(context, str);
        if (!emaFile.exists()) {
            return arrayList;
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        try {
            java.io.FileReader fileReader = new java.io.FileReader(emaFile);
            try {
                char[] cArr = new char[4096];
                while (true) {
                    int read = fileReader.read(cArr);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(cArr, 0, read);
                }
                org.json.JSONArray jSONArray = new org.json.JSONArray(sb.toString());
                for (i = 0; i < jSONArray.length(); i++) {
                    org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        arrayList.add(com.echoflow.chat.EmaStore.Ema.fromJson(optJSONObject));
                    }
                }
                fileReader.close();
            } finally {
            }
        } catch (java.lang.Exception unused) {
        }
        arrayList.sort(new java.util.Comparator() { // from class: com.echoflow.chat.EmaStore$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(java.lang.Object obj, java.lang.Object obj2) {
                int compare;
                compare = java.lang.Long.compare(((com.echoflow.chat.EmaStore.Ema) obj2).createdAt, ((com.echoflow.chat.EmaStore.Ema) obj).createdAt);
                return compare;
            }
        });
        return arrayList;
    }

    public static void saveAll(android.content.Context context, java.lang.String str, java.util.List<com.echoflow.chat.EmaStore.Ema> list) {
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        java.util.Iterator<com.echoflow.chat.EmaStore.Ema> it = list.iterator();
        while (it.hasNext()) {
            try {
                jSONArray.put(it.next().toJson());
            } catch (java.lang.Exception unused) {
            }
        }
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(emaFile(context, str));
            try {
                fileWriter.write(jSONArray.toString());
                fileWriter.close();
            } finally {
            }
        } catch (java.lang.Exception unused2) {
        }
    }

    public static com.echoflow.chat.EmaStore.Ema writeUserEma(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, java.lang.String str) {
        com.echoflow.chat.EmaStore.Ema ema = new com.echoflow.chat.EmaStore.Ema();
        ema.id = "ema_u_" + java.lang.System.currentTimeMillis();
        ema.cardId = characterCard.id;
        ema.text = str;
        ema.author = "user";
        ema.createdAt = java.lang.System.currentTimeMillis();
        java.util.List<com.echoflow.chat.EmaStore.Ema> list = list(context, characterCard.id);
        list.add(ema);
        saveAll(context, characterCard.id, list);
        com.echoflow.chat.Memory memory = new com.echoflow.chat.Memory();
        memory.id = "mem_ema_" + ema.id;
        memory.cardId = characterCard.id;
        memory.content = "你把愿望写在绘马上了：「" + str + "」。";
        memory.importance = 4;
        memory.category = "relationship";
        memory.source = "银月神社 · 绘马架";
        memory.time = java.lang.System.currentTimeMillis();
        com.echoflow.chat.MemoryStore.add(context, characterCard.id, memory);
        return ema;
    }

    public static com.echoflow.chat.EmaStore.Ema writeHerEma(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, java.lang.String str) {
        com.echoflow.chat.EmaStore.Ema ema = new com.echoflow.chat.EmaStore.Ema();
        ema.id = "ema_h_" + java.lang.System.currentTimeMillis();
        ema.cardId = characterCard.id;
        ema.text = str;
        ema.author = "character";
        ema.createdAt = java.lang.System.currentTimeMillis();
        java.util.List<com.echoflow.chat.EmaStore.Ema> list = list(context, characterCard.id);
        list.add(ema);
        saveAll(context, characterCard.id, list);
        return ema;
    }

    public static void delete(android.content.Context context, java.lang.String str, final java.lang.String str2) {
        java.util.List<com.echoflow.chat.EmaStore.Ema> list = list(context, str);
        list.removeIf(new java.util.function.Predicate() { // from class: com.echoflow.chat.EmaStore$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(java.lang.Object obj) {
                boolean equals;
                equals = ((com.echoflow.chat.EmaStore.Ema) obj).id.equals(str2);
                return equals;
            }
        });
        saveAll(context, str, list);
    }
}
