package hu.merenyimiklos.meterreader.update

internal object VersionComparator {
    fun isNewer(
        current: String,
        latest: String
    ): Boolean =
        compare(
            latest,
            current
        ) > 0

    fun compare(
        left: String,
        right: String
    ): Int {
        val leftVersion =
            parse(left)
        val rightVersion =
            parse(right)

        if (
            leftVersion == null ||
            rightVersion == null
        ) {
            return left.compareTo(
                right,
                ignoreCase = true
            )
        }

        for (
            index in
            leftVersion.numbers.indices
        ) {
            val diff =
                leftVersion.numbers[index]
                    .compareTo(
                        rightVersion
                            .numbers[index]
                    )
            if (diff != 0) {
                return diff
            }
        }

        val leftPre =
            leftVersion.prerelease
        val rightPre =
            rightVersion.prerelease

        if (
            leftPre == null &&
            rightPre == null
        ) {
            return 0
        }
        if (leftPre == null) {
            return 1
        }
        if (rightPre == null) {
            return -1
        }

        val leftParts =
            leftPre.split('.')
        val rightParts =
            rightPre.split('.')
        val count =
            maxOf(
                leftParts.size,
                rightParts.size
            )

        for (index in 0 until count) {
            val leftPart =
                leftParts.getOrNull(index)
                    ?: return -1
            val rightPart =
                rightParts.getOrNull(index)
                    ?: return 1

            val leftNumber =
                leftPart.toIntOrNull()
            val rightNumber =
                rightPart.toIntOrNull()

            val diff =
                when {
                    leftNumber != null &&
                        rightNumber != null ->
                        leftNumber.compareTo(
                            rightNumber
                        )

                    leftNumber != null ->
                        -1

                    rightNumber != null ->
                        1

                    else ->
                        leftPart.compareTo(
                            rightPart,
                            ignoreCase = true
                        )
                }

            if (diff != 0) {
                return diff
            }
        }

        return 0
    }

    private fun parse(
        value: String
    ): ParsedVersion? {
        val match =
            REGEX.matchEntire(
                value.trim()
            ) ?: return null

        val numbers =
            (1..4).map { group ->
                match.groupValues[group]
                    .toIntOrNull()
                    ?: 0
            }

        val prerelease =
            match.groupValues[5]
                .ifBlank {
                    null
                }

        return ParsedVersion(
            numbers = numbers,
            prerelease = prerelease
        )
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val prerelease: String?
    )

    private val REGEX =
        Regex(
            """^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?$"""
        )
}
