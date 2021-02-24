/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.model

import java.time.Instant

/**
 * Provenance information about the scanned source code. Either [sourceArtifact] or [vcsInfo] can be set to a non-null
 * value. If both are null this indicates that no provenance information is available.
 */
sealed class Provenance {
    /**
     * The time when the source code was downloaded, or [Instant.EPOCH] if unknown (e.g. for source code that was
     * downloaded separately from running ORT).
     */
    val downloadTime: Instant = Instant.EPOCH

    /**
     * True if this [Provenance] refers to the same source code as [pkg], assuming that it belongs to the package id.
     */
    abstract fun matches(pkg: Package): Boolean

    data class Artifact(
        /**
         * The source artifact that was downloaded.
         */
        val sourceArtifact: RemoteArtifact
    ) : Provenance() {
        override fun matches(pkg: Package): Boolean {
            return sourceArtifact == pkg.sourceArtifact
        }
    }

    data class Repository(
        /**
         * The VCS repository that was downloaded.
         */
        val vcsInfo: VcsInfo,

        /**
         * The original [VcsInfo] that was used to download the source code. It can be different to [vcsInfo] if any
         * automatic detection took place. For example if the original [VcsInfo] does not contain any revision and the
         * revision was automatically detected by searching for a tag that matches the version of the package there
         * would be no way to match the package to the [Provenance] without downloading the source code and searching
         * for the tag again.
         */
        val originalVcsInfo: VcsInfo
    ) : Provenance() {
        override fun matches(pkg: Package): Boolean {
            // If no VCS information is present either, or it does not have a resolved revision, there is no way of
            // verifying matching provenance.
            if (vcsInfo.resolvedRevision == null) {
                return false
            }

            // If pkg.vcsProcessed equals originalVcsInfo or vcsInfo this provenance was definitely created when downloading
            // this package.
            if (pkg.vcsProcessed == originalVcsInfo || pkg.vcsProcessed == vcsInfo) {
                return true
            }

            return listOf(pkg.vcs, pkg.vcsProcessed).any {
                if (it.resolvedRevision != null) {
                    it.resolvedRevision == vcsInfo.resolvedRevision
                } else {
                    it.revision == vcsInfo.revision || it.revision == vcsInfo.resolvedRevision
                } && it.type == vcsInfo.type && it.url == vcsInfo.url && it.path == vcsInfo.path
            }
        }
    }
}
