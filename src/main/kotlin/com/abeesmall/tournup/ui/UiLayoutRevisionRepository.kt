package com.abeesmall.tournup.ui

import org.springframework.data.jpa.repository.JpaRepository

interface UiLayoutRevisionRepository :
    JpaRepository<UiLayoutRevisionRecord, UiLayoutRevisionId>
