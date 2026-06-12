-- Resynchronise les séquences après inserts manuels (seeds V3/V11).
-- Évite les doublons DOS-2026-0000N / REC-2026-0000N / GNB-YYYY-0000N à la création via l'API.

SELECT setval(
    'dossier_seq',
    GREATEST(
        COALESCE(
            (SELECT MAX((regexp_match(reference, '^DOS-\d{4}-(\d+)$'))[1]::bigint) FROM dossiers),
            0
        ),
        COALESCE((SELECT last_value FROM dossier_seq), 0)
    )
);

SELECT setval(
    'encaissement_seq',
    GREATEST(
        COALESCE(
            (SELECT MAX((regexp_match(numero_recu, '^REC-\d{4}-(\d+)$'))[1]::bigint) FROM encaissements),
            0
        ),
        COALESCE((SELECT last_value FROM encaissement_seq), 0)
    )
);

SELECT setval(
    'usager_seq',
    GREATEST(
        COALESCE(
            (SELECT MAX((regexp_match(numero, '^[A-Z]+-\d{4}-(\d+)$'))[1]::bigint) FROM usagers),
            0
        ),
        COALESCE((SELECT last_value FROM usager_seq), 0)
    )
);
