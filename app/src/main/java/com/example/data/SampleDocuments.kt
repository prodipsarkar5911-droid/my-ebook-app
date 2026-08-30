package com.example.data

data class SampleDocument(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val chapterTitle: String,
    val subtitle: String,
    val sampleParagraphs: List<String>,
    val hasColorCallout: Boolean,
    val fontNote: String?
)

object SampleDocuments {
    val SAMPLES = listOf(
        SampleDocument(
            id = "classic_novel",
            title = "The Time Machine (Scanned Novel)",
            category = "Classic Literature",
            description = "Scanned book page with chapter header, multiple indented paragraphs, and author dialog.",
            chapterTitle = "CHAPTER I: THE MACHINE",
            subtitle = "An Exposition of the Fourth Dimension",
            sampleParagraphs = listOf(
                "The Time Traveller (for so it will be convenient to speak of him) was expounding a recondite matter to us. His grey eyes shone and twinkled, and his usually pale face was flushed and animated.",
                "The fire burnt brightly, and the soft radiance of the incandescent lights in the lilies of silver caught the bubbles that flashed and passed in our glasses. Our chairs, being his patents, embraced and caressed us rather than submitted to be sat upon.",
                "‘You must follow me carefully. I shall have to controvert one or two ideas that are almost universally accepted. The geometry, for instance, they taught you at school is founded on a misconception.’",
                "‘Is not that rather a large thing to expect us to begin upon?’ said Filby, an argumentative person with red hair. ‘I do not mean to ask you to accept anything without reasonable ground for it,’ replied the Time Traveller."
            ),
            hasColorCallout = false,
            fontNote = null
        ),
        SampleDocument(
            id = "botany_guide",
            title = "Field Guide to Flora (Multi-Font & Colors)",
            category = "Botanical Study",
            description = "Scientific monograph featuring colored classification tags and distinct italicized font families.",
            chapterTitle = "TAXONOMIC RECORD 42: FERNS",
            subtitle = "Morphology of Polypodiopsida",
            sampleParagraphs = listOf(
                "Ferns are vascular plants that reproduce via spores and have neither seeds nor flowers. They differ from mosses by being vascular, possessing specialized tissues that conduct water and nutrients.",
                "WARNING: Specimen collected under sub-alpine humidity. Spore release mechanism exhibits distinct hygroscopic tension requiring immediate botanical containment.",
                "The fronds in mature specimens range from thirty to eighty centimeters in length, showing bipinnate segmentation along a dark rachis."
            ),
            hasColorCallout = true,
            fontNote = "Herbarium Reference: Specimen preserved under Monotype Baskerville catalogue series #1892."
        ),
        SampleDocument(
            id = "astronomy_treatise",
            title = "Treatise on Celestial Motion (1888)",
            category = "Archival Treatise",
            description = "Archival astronomical document with mathematical subheadings, numbered observations, and Latin references.",
            chapterTitle = "TREATISE ON ORBITAL DYNAMICS",
            subtitle = "Section III: Perturbations in Planetary Ellipses",
            sampleParagraphs = listOf(
                "In calculating the secular variations of the orbital eccentricity, the mutual gravitational attraction between adjacent planetary masses must be integrated over extended chronological epochs.",
                "Observational data gathered across twenty-four lunar cycles demonstrate a periodic oscillation corresponding to the theoretical perturbation predicted by the Laplacian model.",
                "The observer must take diligent account of atmospheric refraction when the celestial body approaches the local meridian within fifteen degrees of the horizon."
            ),
            hasColorCallout = false,
            fontNote = "Archival Codex: Marginalia annotated in Victorian Copperplate script."
        )
    )
}
