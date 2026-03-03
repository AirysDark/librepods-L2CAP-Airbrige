    private fun resToUri(resId: Int): Uri? {
        return try {
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority("me.kavishdevar.librepods")
                .appendPath(applicationContext.resources.getResourceTypeName(resId))
                .appendPath(applicationContext.resources.getResourceEntryName(resId))
                .build()
        } catch (_: Resources.NotFoundException) {
            null
        }
    }
