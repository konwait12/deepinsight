# -*- coding: utf-8 -*-
import json, os, sys
import mysql.connector
conn = mysql.connector.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database=os.getenv("DB_NAME", "deepinsight"),
)
cur = conn.cursor()
data = json.loads(open(sys.argv[1], encoding="utf-8").read())
for d in data:
    cur.execute("INSERT IGNORE INTO knowledge_articles (node_id,title,content,author_id,is_pinned) VALUES (%s,%s,%s,1,0)", (d["n"],d["t"],d["c"]))
conn.commit()
cur.execute("SELECT COUNT(*) FROM knowledge_articles")
print(f"Total: {cur.fetchone()[0]}")
conn.close()
