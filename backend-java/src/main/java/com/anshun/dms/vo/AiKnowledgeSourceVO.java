package com.anshun.dms.vo;

/** A server-selected source used for a RAG answer; page numbers are null for text files. */
public record AiKnowledgeSourceVO(Long documentId, String title, Integer pageStart, Integer pageEnd, String excerpt) { }
