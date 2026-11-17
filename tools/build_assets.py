#!/usr/bin/env python3
"""Build the bundled data/ assets from the class export.

Editorial rules baked in here (do not undo them when refreshing a drop):

- Commute copy names only what Eric said: JR A-line, 茨木駅 to 大阪駅,
  the rapid if he catches it, then underground exit M16 and E Street to
  the YWCA. Do not call the destination Umeda. Do not name intermediate
  stations (he has not said which ones his train stops at). 吹田 and 梅田
  stay in the word list only as words that appeared on a handout.
- 頻率 on the 2026-10-20 sheet was the teacher's Chinese gloss for
  Chinese-speaking classmates, not Eric's mistake. 頻度 is ordinary class
  vocabulary (status known). No error drill on ひんどう / 頻率.
- Do not invent extra weak spots from handwritten multilingual glosses.
  The only biased drills are the ones he confirmed: ひま vs たいくつ,
  物価 vs 値段, small kana (ディ), and おもしろかった.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "data"

# word -> (gloss_ja, optional watch_out ruby, optional extra accepts)
GLOSSES: dict[str, tuple[str, str | None, list[str]]] = {}


def g(word: str, gloss: str, watch: str | None = None, accept: list[str] | None = None) -> None:
    GLOSSES[word] = (gloss, watch, accept or [])


g("夕日", "ひが しずむ ときの、あかい おひさま。")
g("万里の長城", "ちゅうごくの きたの ほうに ある、とても ながい かべ。")
g("北東", "きたと ひがしの あいだの ほう。")
g("厳しい", "ルールや ことばが きつくて、ゆるくない。")
g("親切な", "ひとが やさしくて、よく てつだって くれる。", accept=["しんせつ"])
g("楽な", "らくで、つかれにくい。むずかしくない。", accept=["らく"])
g("暇な", "いそがしくない。じかんが ある。", "いそがしくない、ということ。たいくつ（bored）とは ちがう。", ["ひま"])
g("生活", "まいにちの くらし。")
g("匂い", "はなで かんじる もの。いい においも、ふつうの においも。")
g("昨夜", "きのうの よる。")
g("看護師", "びょういんで、ぐあいの わるい ひとの せわを する ひと。")
g("通訳", "はなしている ことばを、ほかの ことばに かえる ひと。")
g("警官", "まちの あんぜんを まもる ひと。")
g("物価", "まち ぜんたいで、ものの ねだんが たかいか やすいか。", "ひとつの ものの ねだんは 値段[ねだん]。", ["ぶっか"])
g("値段", "その ひとつの ものの ねだん。", "まち ぜんたいの たかさは 物価[ぶっか]。", ["ねだん"])
g("実際", "そう かもしれない、では なく、ほんとに。")
g("どこか", "ばしょは ひとつに きまっていない。「どこか へ いく」。")
g("低い", "たかさが すくない。たかい の はんたい。")
g("自己紹介", "じぶんの なまえや くらしを、ひとに はなす こと。")
g("名前", "その ひとや ものを よぶ なまえ。")
g("国", "にほんや アメリカの ような、おおきな くに。")
g("町", "ひとが すむ まち。")
g("フルネーム", "みょうじと なまえ、ぜんぶ。")
g("ニックネーム", "ともだちが よぶ、みじかい なまえ。")
g("お宅", "あいての いえを、ていねいに いう ことば。")
g("電車", "レールの うえを はしる のりもの。")
g("仕事", "おかねを もらう ために する こと。")
g("誕生日", "その ひとが うまれた ひ。")
g("〜歳", "なんさいか、という かぞえかた。", accept=["さい"])
g("書いてください", "じを かいて、と おねがいする とき。")
g("先生", "クラスで おしえて くれる ひと。")
g("学生", "がっこうで べんきょうしている ひと。")
g("医者", "びょうきを みて くれる ひと。")
g("歯医者", "はの いしゃ。")
g("主婦", "いえの ことを おもに する ひと。")
g("会社員", "かいしゃで はたらく ひと。")
g("店員", "みせで ものを うる ひと。")
g("駅員", "えきで はたらく ひと。")
g("歌手", "うたを うたう しごとを する ひと。")
g("趣味", "しごとでは なく、すきで する こと。")
g("映画", "おおきい がめんで みる はなし。")
g("見ます", "めで みる。えいがを みる、とも。")
g("香港", "ホンコン。ちゅうごくの みなみの まち。")
g("ラブストーリー", "こいの はなし。")
g("好き", "それが いい、と おもう きもち。")
g("頻度", "どのくらいの あいだに、なんかい するか。")
g("いつも", "どの ときも。ずっと そうする。")
g("よく", "おおくの ときに。たびたび。")
g("時々", "いつもでは ないが、なんどか。")
g("あまり〜ません", "「あまり」と、ない かたち。すこししか しない。")
g("全然〜ません", "「ぜんぜん」と、ない かたち。いちども しない。")
g("お腹が空きました", "おなかが すいた、と いう とき。")
g("ご飯", "りょうりした こめ。しょくじの ことも いう。")
g("食べます", "くちに いれて たべる。")
g("レストラン", "がいしゅつして ごはんを たべる みせ。")
g("入ります", "なかへ はいる。")
g("一緒に", "ひとりでは なく、だれかと。")
g("ハンバーガー", "パンに にくを はさんだ たべもの。")
g("寿司", "さかななどを こめと あわせた たべもの。")
g("ラーメン", "あじの ついた スープの めん。")
g("旅行します", "とおくへ あそびに いく。")
g("京都", "きょうと。ふるい まちで しられる。")
g("遊びます", "たのしむ。スポーツを する、とも。")
g("テニス", "ラケットで ボールを うつ スポーツ。")
g("ちょっと…", "はっきり いやと いわず、ことわる ときの ことば。", accept=["ちょっと"])
g("眠い", "ねたい きもち。")
g("寝ます", "ベッドに はいって ねる。")
g("遅い", "じかんが おそい。スピードが おそい、とも。")
g("起きます", "ねた あと、おきる。")
g("働きます", "しごとを する。")
g("勉強します", "あたらしい ことを ならう。")
g("授業", "がっこうの クラスの じかん。")
g("〜ごろ", "その じかんの あたり。「7じごろ」。", accept=["ごろ"])
g("毎日", "いちにち いちにち、ぜんぶ。")
g("毎週", "いっしゅうかん ごとに。")
g("毎月", "ひとつき ごとに。")
g("昨日", "きょうの まえの ひ。")
g("今日", "いまの この ひ。")
g("明日", "きょうの つぎの ひ。")
g("今朝", "きょうの あさ。")
g("先週", "いまの しゅうの まえの しゅう。")
g("今週", "いまの この しゅう。")
g("来週", "いまの しゅうの つぎの しゅう。")
g("先月", "いまの つきの まえの つき。")
g("今月", "いまの この つき。")
g("来月", "いまの つきの つぎの つき。")
g("去年", "ことしの まえの とし。")
g("今年", "いまの この とし。")
g("来年", "ことしの つぎの とし。")
g("結婚します", "ふうふに なる。")
g("飲みます", "くちから のみこむ。みずや おちゃを のむ。")
g("お誕生日おめでとう", "たんじょうびに いう あいさつ。")
g("プレゼント", "ひとに あげる もの。")
g("綺麗な", "みて、うつくしい。", accept=["きれい"])
g("花", "きに さく、いろの ある もの。")
g("何て言うんですか", "なまえが わからない とき、なんと いうか きく。")
g("薔薇", "バラ。とげの ある はな。")
g("本当に", "うそでは なく。とても、と つよめる ときも。")
g("初めて", "いちばん はじめの とき。")
g("〜回目", "なんかいめ、という かぞえかた。", accept=["かいめ"])
g("面白い", "ききたい、みたい、と おもう。たのしい。")
g("食べ物", "たべる もの。")
g("美味しい", "あじが いい。")
g("形容詞", "ようすを いう ことば。あつい、きれい、など。")
g("高い", "ねだんが おおい。たかさが ある、とも。")
g("元気", "からだも きもちも よい。")
g("暑い", "てんきや くうきが あつい。")
g("名詞", "ものの なまえに なる ことば。")
g("お国", "あいての くにを、ていねいに いう ことば。")
g("中国", "ちゅうごく。アジアの おおきな くに。")
g("北京", "ペキン。ちゅうごくの みやこ。")
g("古い", "できて から、ながい じかんが たっている。")
g("有名", "しっている ひとが おおい。")
g("北京ダック", "ペキンの あひるの りょうり。")
g("富士山", "にほんの たかい やま、ふじさん。")
g("山", "まわりより たかい じめん。")
g("方位", "きた、みなみ、ひがし、にしの ほうこう。")
g("北", "きた。おひさまの ひがしから みて、ひだりの ほう。")
g("南", "みなみ。きたの はんたい。")
g("東", "ひがし。おひさまが のぼる ほう。")
g("西", "にし。おひさまが しずむ ほう。")
g("北西", "きたと にしの あいだ。")
g("南東", "みなみと ひがしの あいだ。")
g("南西", "みなみと にしの あいだ。")
g("デート", "すきな ひとと であう やくそく。")
g("楽しい", "うれしくて、わらう。たのしい。")
g("パーティ", "みんなで あつまる たのしい かい。")
g("テスト", "どれだけ わかったか、しらべる もの。")
g("難しい", "かんたんでは ない。")
g("忙しい", "やることが おおくて、じかんが ない。")
g("良かった", "「いい」の かこ。よかった。")
g("会います", "ひとと であう。")
g("日曜日", "いっしゅうかんの なかの、にちようび。")
g("家", "すんでいる いえ。")
g("登ります", "したから うえへ のぼる。")
g("大変な", "くるしくて、ほねが おれる。", accept=["たいへん"])
g("寒い", "てんきが さむい。")
g("暖かい", "さむくも あつくも ない。ぽかぽか する。")
g("だから", "その りゆうで。なので。")
g("冬", "いちねんの なかで、さむい きせつ。")
g("嫌い", "すきでは ない。")
g("マイナス", "れいより すくない かず。ひくい おんど。")
g("〜度", "おんどや かくどの たんい。", accept=["ど"])
g("〜ぐらい", "だいたい そのくらい。", accept=["ぐらい"])
g("へえ", "おどろいた ときに いう ことば。")
g("西瓜", "なつに たべる、あまくて みずけの おおい くだもの。")
g("林檎", "まるい くだもの。あかい ことが おおい。")
g("牛肉", "うしの にく。")
g("鶏肉", "にわとりの にく。")
g("バイク", "エンジンの ついた にりんしゃ。")
g("自転車", "ペダルを こいで のる のりもの。")
g("奈良", "なら。ふるい おてらで しられる まち。")
g("北海道", "にほんの きたの おおきな しま。")
g("沖縄", "にほんの みなみの しまぐに。")
g("同じぐらい", "くらべて、ほとんど おなじ。")
g("人口", "その まちに すんでいる ひとの かず。")
g("多い", "かずが おおい。")
g("大きい", "サイズが おおきい。")
g("賑やかな", "ひとが おおくて、にぎやか。", accept=["にぎやか"])
g("天ぷら", "さかなや やさいに ころもを つけて あげた たべもの。")
g("どちら", "ふたつの うち、どっち。")
g("〜の方", "くらべた とき、その ほう。", accept=["のほう", "ほう"])
g("どちらも", "ふたつの りょうほう。")
g("犬", "いぬ。ひとと くらす ことが おおい どうぶつ。")
g("猫", "ねこ。にゃあと なく どうぶつ。")
g("紅茶", "あかい いろの おちゃ。")
g("パン", "こむぎから やいた たべもの。")
g("海", "しおの みずが ひろがっている ところ。")
g("サッカー", "あしで ボールを ける スポーツ。")
g("夏", "いちねんの なかで、あつい きせつ。")
g("土曜日", "にちようびの まえの ひ。")
g("一番", "グループの なかで、もっとも。")
g("果物", "りんごや みかんの ような、あまい み。")
g("お菓子", "ケーキや クッキーの ような、あまい たべもの。")
g("動物", "いぬ、ねこ、とりの ような いきもの。")
g("スポーツ", "からだを うごかして する あそびや しあい。")
g("今度", "このつぎ。もうすぐ くる とき。")
g("別に", "「べつに」と ない かたちで、とくには ない、という とき。")
g("日本料理", "にほんの たべもの。すしや てんぷらなど。")
g("店", "ものを うったり、ごはんを だしたり する ところ。")
g("欲しい", "それが ほしい、と おもう。")
g("冷蔵庫", "たべものを ひやして おく はこ。")
g("机", "べんきょうや しごとを する つくえ。")
g("車", "ロードを はしる のりもの。")
g("電子レンジ", "たべものを あたためる きかい。")
g("〜億円", "とても おおきな おかねの かぞえかた。1おくは 1と ゼロが 8こ。", accept=["おくえん"])
g("吹田", "クラスの プリントに でてきた、まちの なまえ。エリックの のりものでは つかわない。")
g("止まります", "うごいて いた ものが、とまる。")
g("次", "いまの つぎ。")
g("普通", "とくべつでは ない こと。でんしゃの なまえの ときは、ふつうの れっしゃ。")
g("乗ります", "のって いく。")
g("降ります", "のりものから おりる。")
g("梅田", "クラスの プリントに でてきた、ばしょの なまえ。エリックが おりる えきの なまえでは ない。")
g("ボールペン", "さきが ボールの ペン。")
g("貸します", "あとで かえして もらう つもりで、わたす。")
g("動詞", "することを いう ことば。たべる、いく、など。")
g("て形", "どうしの かたちを かえた もの。「たべて」「のんで」など。")
g("グループ", "いくつかの ものを まとめた くみ。")
g("書きます", "じを かく。")
g("泳ぎます", "みずの なかを およぐ。")
g("持ちます", "てに もつ。")
g("帰ります", "いえに もどる。")
g("死にます", "いきものが しぬ。")
g("話します", "ことばを はなす。")
g("開けます", "しまって ある ものを あける。")
g("閉めます", "あいている ものを しめる。")
g("聞きます", "みみで きく。")
g("宿題", "いえで する べんきょう。")
g("出します", "なかから だす。しゅくだいを わたす、とも。")
g("読みます", "じを よむ。")
g("ホワイトボード", "しろい いたに、マーカーで かく。")
g("学校", "べんきょうしに いく ところ。")
g("病院", "びょうきの とき、いしゃに みてもらう ところ。")
g("口", "たべたり はなしたり する、かおの くち。")
g("野菜", "にんじんや キャベツの ような たべもの。")
g("薬", "びょうきの ときに のむ もの。")
g("早く", "じかんが はやい。スピードが はやい、とも。")
g("空港", "ひこうきが でる ところ。")
g("パスポート", "くにの そとに いく ときに もつ てちょう。")
g("見せます", "ひとに みせる。")
g("鞄", "ものを いれて もつ かばん。")
g("タクシー", "おかねを はらって のる くるま。")
g("美容院", "かみを きったり ととのえたり する みせ。びょういんとは ちがう。")
g("切ります", "はさみや ナイフで きる。")
g("センチ", "ながさの たんい。")
g("駅", "でんしゃが とまる ところ。")
g("窓", "ひかりや かぜが はいる、かべの あな。")
g("電気", "あかり。スイッチで つける。")
g("付けます", "でんきなどを つける。")
g("消します", "でんきなどを けす。")
g("ドア", "へやに はいる とびら。")
g("ゆっくり", "スピードを おとして。あわてないで。")
g("撮ります", "カメラで しゃしんを とる。")
g("写真", "カメラで とった え。")
g("傘", "あめの ときに さす もの。")
g("教えます", "しらない ひとに、やりかたや みちを はなす。")
g("お金", "かう ときに はらう もの。")


def gp(
    gid: str,
    pattern: str,
    explanation_ja: str,
    explanation_en: str,
    example: str,
    example_en: str,
    cloze: str,
    answer: str,
    accept: list[str] | None = None,
    hint: str | None = None,
    weak: str | None = None,
    mistakes: list[dict] | None = None,
) -> dict:
    item = {
        "id": gid,
        "pattern_ruby": pattern,
        "explanation_ja": explanation_ja,
        "explanation_en": explanation_en,
        "example_ruby": example,
        "example_en": example_en,
        "cloze_ruby": cloze,
        "cloze_answer": answer,
        "cloze_accept": accept or [answer],
        "cloze_hint_en": hint or example_en,
    }
    if weak:
        item["weak_spot_id"] = weak
    if mistakes:
        item["mistakes"] = mistakes
    return item


M_OMOSHIRO = [
    {"answer": "おもしかった", "hint_en": "The ろ stays. おもしろい → おもしろかった."},
    {"answer": "おもしかったです", "hint_en": "The ろ stays. おもしろい → おもしろかった."},
    {"answer": "おもしろいだった", "hint_en": "い-adjectives do not take だった. おもしろい → おもしろかった."},
]

GRAMMAR: dict[str, list[dict]] = {
    "2026-10-16": [
        gp(
            "g-1016-01",
            "〜と よんでください",
            "なまえを、こう よんでね、と おねがいする。",
            "Please call me ~.",
            "エリックと よんでください。",
            "Please call me Eric.",
            "エリックと ____。",
            "よんでください",
            ["よんでください", "よんでください。"],
        ),
        gp(
            "g-1016-02",
            "[くに]の [まち]から きました",
            "くにの まちから きた、と いう。",
            "I came from (city) in (country).",
            "アメリカの サンディエゴから きました。",
            "I came from San Diego, in the US.",
            "アメリカの ____ から きました。",
            "サンディエゴ",
            ["サンディエゴ"],
            "San Diego. Small ディ, not ジ.",
            "small-kana",
            [
                {"answer": "サンジエゴ", "hint_en": "Small ィ. ディ, not a big ジ."},
                {"answer": "さんじえご", "hint_en": "Small ィ. サンディエゴ."},
            ],
        ),
        gp(
            "g-1016-03",
            "いつ 日本[にほん]に きましたか",
            "きた ときを きく。こたえる ときは 「〜ねんに きました」。",
            "When did you come to Japan? / I came in ~.",
            "2020ねんに きました。",
            "I came in 2020.",
            "2020ねんに ____。",
            "きました",
            ["きました", "きました。"],
        ),
        gp(
            "g-1016-04",
            "[のりもの]で 行[い]きます",
            "で は、のりもの。エリックは JRの エーせんで、いばらきえきから おおさかえきまで いく。",
            "で marks the transport. Eric rides the JR A-line from Ibaraki Station to Osaka Station.",
            "A[エー]線[せん]で、茨木駅[いばらきえき]から 大阪駅[おおさかえき]まで 行[い]きます。",
            "I go from Ibaraki Station to Osaka Station on the A-line.",
            "茨木駅[いばらきえき]から 大阪駅[おおさかえき]まで、____ で 行[い]きます。",
            "でんしゃ",
            ["でんしゃ", "電車"],
            "By train, from Ibaraki Station to Osaka Station.",
        ),
        gp(
            "g-1016-05",
            "すみません、書[か]いてください",
            "クラスで、じを かいて、と たのむ。",
            "Excuse me, please write it down.",
            "すみません、書[か]いてください。",
            "Excuse me, please write it.",
            "すみません、____。",
            "かいてください",
            ["かいてください", "書いてください"],
        ),
    ],
    "2026-10-20": [
        gp(
            "g-1020-01",
            "Nの 趣味[しゅみ]は なんですか",
            "その ひとの しゅみを きく。",
            "What is (someone)'s hobby?",
            "Bさんの 趣味[しゅみ]は なんですか。",
            "What is your hobby?",
            "Bさんの ____ は なんですか。",
            "しゅみ",
            ["しゅみ", "趣味"],
        ),
        gp(
            "g-1020-02",
            "どんな Nを Vますか",
            "どんな しゅるいか、きく。",
            "What kind of N do you V?",
            "どんな 映画[えいが]を 見[み]ますか。",
            "What kind of movies do you watch?",
            "どんな えいがを ____ か。",
            "みます",
            ["みます", "見ます"],
        ),
        gp(
            "g-1020-03",
            "いつも・よく・時々[ときどき] / あまり・全然[ぜんぜん]",
            "いつも、よく、ときどき は する かたち。あまり と ぜんぜん は ない かたち。",
            "Always, often, and sometimes take ます. あまり and ぜんぜん need a negative.",
            "わたしは あまり 映画[えいが]を 見[み]ません。",
            "I don't watch movies much.",
            "わたしは あまり えいがを ____。",
            "みません",
            ["みません", "見ません"],
        ),
        gp(
            "g-1020-04",
            "Nも です",
            "も は、おなじ、わたしも、という とき。",
            "も means also / too.",
            "わたしも です。",
            "Me too.",
            "わたし____ です。",
            "も",
            ["も"],
        ),
    ],
    "2026-10-23": [
        gp(
            "g-1023-01",
            "Vませんか",
            "いっしょに しない、と さそう。",
            "Invitation: won't you ~?",
            "一緒[いっしょ]に ご飯[はん]を 食[た]べませんか。",
            "Won't you have a meal with me?",
            "いっしょに ごはんを ____。",
            "たべませんか",
            ["たべませんか", "食べませんか"],
        ),
        gp(
            "g-1023-02",
            "ええ、そうしましょう / Vましょう",
            "さそいに のる。しましょう、は いっしょに やろう。",
            "Accepting, or let's ~.",
            "ハンバーガーを 食[た]べましょう。",
            "Let's eat hamburgers.",
            "ハンバーガーを ____。",
            "たべましょう",
            ["たべましょう", "食べましょう"],
        ),
        gp(
            "g-1023-03",
            "Nは どうですか",
            "これを どう、と ていあんする。",
            "How about N?",
            "あそこの レストランは どうですか。",
            "How about that restaurant?",
            "あそこの レストランは ____。",
            "どうですか",
            ["どうですか"],
        ),
        gp(
            "g-1023-04",
            "すみません、Nは ちょっと…",
            "はっきり いわず、ことわる。",
            "A soft no, trailing off.",
            "すみません、寿司[すし]は ちょっと…",
            "Sorry, sushi is a bit…",
            "すみません、すしは ____",
            "ちょっと…",
            ["ちょっと…", "ちょっと", "ちょっと..."],
        ),
        gp(
            "g-1023-05",
            "[じかん]に V",
            "じこく、ひづけ には に を つける。きのう、まいにち、らいしゅう には に を つけない。〜ごろ は その あたり。",
            "に marks clock times and dates. きのう, まいにち, らいしゅう take no に.",
            "毎日[まいにち] 11じに 寝[ね]ます。",
            "I go to bed at 11 every day.",
            "まいにち 11じ____ ねます。",
            "に",
            ["に"],
        ),
        gp(
            "g-1023-06",
            "〜から 〜まで",
            "はじめと おわり。",
            "From ~ until ~.",
            "12じから 4じまで 勉強[べんきょう]しました。",
            "I studied from 12 to 4.",
            "12じ____ 4じまで べんきょうしました。",
            "から",
            ["から"],
        ),
        gp(
            "g-1023-07",
            "Vました / Vませんでした",
            "ます は いまや これから。ました は むかし。",
            "Non-past versus past.",
            "昨夜[ゆうべ] 11じに 寝[ね]ました。",
            "I went to bed at 11 last night.",
            "ゆうべ 11じに ____。",
            "ねました",
            ["ねました", "寝ました"],
        ),
        gp(
            "g-1023-08",
            "Nではありません",
            "それは Nでは ない、と ていせいする。",
            "Correcting: it is not N.",
            "昨日[きのう]では ありません。今朝[けさ]、4じに 寝[ね]ました。",
            "Not yesterday. I went to bed at 4 this morning.",
            "きのう____ ありません。",
            "では",
            ["では", "じゃ"],
        ),
    ],
    "2026-10-27": [
        gp(
            "g-1027-01",
            "Aい / Aな + N",
            "なまえの まえ。い-けいようしは そのまま。な-けいようしは な を つける。",
            "Before a noun: い-adjectives as they are, な-adjectives add な.",
            "綺麗[きれい]な 花[はな]ですね。",
            "What a pretty flower.",
            "____ はなですね。",
            "きれいな",
            ["きれいな", "綺麗な"],
        ),
        gp(
            "g-1027-02",
            "Aいです → Aくないです",
            "い-けいようしの ひてい。い を くないです に する。",
            "Negative of an い-adjective.",
            "暑[あつ]くないです。",
            "It is not hot.",
            "あつ____です。",
            "くない",
            ["くない"],
        ),
        gp(
            "g-1027-03",
            "Aなです → Aじゃありません",
            "な-けいようしの ひてい。な は つけない。",
            "Negative of a な-adjective.",
            "元気[げんき]じゃ ありません。",
            "I am not well.",
            "げんき____ ありません。",
            "じゃ",
            ["じゃ", "では"],
        ),
        gp(
            "g-1027-04",
            "Nは どうですか",
            "かんそうを きく。",
            "Asking for an impression.",
            "大阪[おおさか]は どうですか。",
            "How is Osaka?",
            "おおさかは ____。",
            "どうですか",
            ["どうですか"],
        ),
        gp(
            "g-1027-05",
            "Nが 有名[ゆうめい]です / Nに あります",
            "で しられている、と ばしょを いう。",
            "Famous for ~, or located in ~.",
            "万里[ばんり]の 長城[ちょうじょう]が 有名[ゆうめい]です。",
            "The Great Wall is famous.",
            "ばんりの ちょうじょうが ____。",
            "ゆうめいです",
            ["ゆうめいです", "有名です"],
        ),
    ],
    "2026-10-30": [
        gp(
            "g-1030-01",
            "N1は どんな N2ですか",
            "どんな しゅるいか きいて、A の N2 で こたえる。",
            "What kind of N2 is N1?",
            "京都[きょうと]は どんな 町[まち]ですか。古[ふる]い 町[まち]です。",
            "What kind of town is Kyoto? An old town.",
            "きょうとは どんな ____ ですか。",
            "まち",
            ["まち", "町"],
        ),
        gp(
            "g-1030-02",
            "Aい → Aかったです / Aくなかったです",
            "い-けいようしの かこ。いい は よかった。おもしろい は おもしろかった。ろ を けさない。",
            "Past of い-adjectives. いい → よかった. おもしろい → おもしろかった. The ろ stays.",
            "映画[えいが]は おもしろかったです。",
            "The movie was interesting.",
            "えいがは ____ です。",
            "おもしろかった",
            ["おもしろかった", "おもしろかったです"],
            "The movie was interesting.",
            "omoshirokatta",
            M_OMOSHIRO,
        ),
        gp(
            "g-1030-03",
            "Aな → Aでした / Aじゃありませんでした",
            "な-けいようしの かこ。な は つけない。",
            "Past of な-adjectives.",
            "暇[ひま]でした。",
            "I was free (not busy).",
            "きのうは ____。",
            "ひまでした",
            ["ひまでした", "暇でした"],
            "I was free, not busy.",
            "hima",
            [{"answer": "たいくつでした", "hint_en": "たいくつ is bored. Free, not busy, is ひま."}],
        ),
        gp(
            "g-1030-04",
            "どうでしたか",
            "かこの かんそうを きく。",
            "How was it?",
            "デートは どうでしたか。",
            "How was the date?",
            "デートは ____。",
            "どうでしたか",
            ["どうでしたか"],
        ),
    ],
    "2026-11-06": [
        gp(
            "g-1106-01",
            "N1は N2より Aです",
            "N1の ほうが、もっと A。",
            "N1 is more A than N2.",
            "北海道[ほっかいどう]は 大阪[おおさか]より 寒[さむ]いです。",
            "Hokkaido is colder than Osaka.",
            "ほっかいどうは おおさか____ さむいです。",
            "より",
            ["より"],
        ),
        gp(
            "g-1106-02",
            "N1と N2と どちらが Aですか",
            "ふたつを くらべる。こたえる ときは、Nのほうが Aです。",
            "Which of the two is more A?",
            "北海道[ほっかいどう]の 方[ほう]が 寒[さむ]いです。",
            "Hokkaido is colder.",
            "ほっかいどうの ____ が さむいです。",
            "ほう",
            ["ほう", "方"],
        ),
        gp(
            "g-1106-03",
            "どちらが 好[す]きですか",
            "ふたつの どちらが すきか。りょうほう なら どちらも。",
            "Which do you prefer? どちらも = both.",
            "寿司[すし]の 方[ほう]が 好[す]きです。",
            "I prefer sushi.",
            "すしの ほうが ____。",
            "すきです",
            ["すきです", "好きです"],
        ),
        gp(
            "g-1106-04",
            "Nと 同[おな]じぐらいです",
            "くらべて、ほとんど おなじ。",
            "About the same as N.",
            "大阪[おおさか]と 同[おな]じぐらいです。",
            "About the same as Osaka.",
            "おおさかと ____ です。",
            "おなじぐらい",
            ["おなじぐらい", "同じぐらい"],
        ),
        gp(
            "g-1106-05",
            "[グループ]の 中[なか]で なにが 一番[いちばん] Aですか",
            "その なかで、もっとも。",
            "The superlative inside a group.",
            "果物[くだもの]の 中[なか]で なにが 一番[いちばん] 好[す]きですか。",
            "Which fruit do you like best?",
            "くだものの なかで なにが ____ すきですか。",
            "いちばん",
            ["いちばん", "一番"],
        ),
    ],
    "2026-11-10": [
        gp(
            "g-1110-01",
            "Nが 欲[ほ]しいです",
            "ものが ほしい。い-けいようしと おなじ かたち。ほしくないです、も。",
            "Want a thing. It behaves like an い-adjective.",
            "車[くるま]が 欲[ほ]しいです。",
            "I want a car.",
            "くるまが ____。",
            "ほしいです",
            ["ほしいです", "欲しいです"],
        ),
        gp(
            "g-1110-02",
            "Vます の なかご + たいです",
            "することを したい。を が が に なる ことが ある。",
            "Want to do. を can become が.",
            "日本[にほん]の 古[ふる]い 町[まち]が 見[み]たいです。",
            "I want to see an old Japanese town.",
            "ふるい まちが ____。",
            "みたいです",
            ["みたいです", "見たいです"],
        ),
        gp(
            "g-1110-03",
            "[ばしょ]に 行[い]きたいです",
            "そこへ いきたい。",
            "Want to go to ~.",
            "京都[きょうと]に 行[い]きたいです。",
            "I want to go to Kyoto.",
            "きょうとに ____。",
            "いきたいです",
            ["いきたいです", "行きたいです"],
        ),
        gp(
            "g-1110-04",
            "どこかに Vませんか",
            "ばしょを きめず さそう。どこに いきましょうか、は ばしょを きく。",
            "Invite someone out, or ask where.",
            "一緒[いっしょ]に どこかに 行[い]きませんか。",
            "Shall we go somewhere together?",
            "いっしょに ____ いきませんか。",
            "どこかに",
            ["どこかに"],
        ),
    ],
    "2026-11-13": [
        gp(
            "g-1113-01",
            "どうしの グループ 1・2・3",
            "1は かきます の ような i-ます。2は たべます、みます の ような もの。3は します と きます。",
            "Group 1 〜i-ます, group 2 〜e-ます (and some 〜i-ます), group 3 します and きます.",
            "書[か]きます、食[た]べます、します。",
            "write, eat, do.",
            "たべます は グループ ____。",
            "2",
            ["2", "に", "２"],
        ),
        gp(
            "g-1113-02",
            "て形[けい]の ルール",
            "き→いて、ぎ→いで、い・ち・り→って、み・に・び→んで、し→して。いきます は いって。",
            "Te-form sound changes. いきます is いって.",
            "飲[の]みます → 飲[の]んで。",
            "drink → te-form.",
            "のみます → ____。",
            "のんで",
            ["のんで", "飲んで"],
        ),
        gp(
            "g-1113-03",
            "Vてください",
            "してください、と せつめいする。",
            "Please do ~ (an instruction).",
            "薬[くすり]を 飲[の]んでください。",
            "Please take the medicine.",
            "くすりを ____。",
            "のんでください",
            ["のんでください", "飲んでください"],
        ),
        gp(
            "g-1113-04",
            "すみません、Vてください",
            "ていねいに たのむ。",
            "A polite request.",
            "すみません、ペンを 貸[か]してください。",
            "Excuse me, could you lend me a pen?",
            "すみません、ペンを ____。",
            "かしてください",
            ["かしてください", "貸してください"],
        ),
        gp(
            "g-1113-05",
            "Nは [ばしょ]に 止[と]まりますか",
            "その のりものが、そこで とまるか きく。エリックの れっしゃが とまる えきは、じぶんが いった ものだけ。いばらきえきから おおさかえきまで。",
            "Does it stop at ~? For Eric's own train, only the stations he named: Ibaraki Station to Osaka Station.",
            "この 快速[かいそく]は 大阪駅[おおさかえき]に 止[と]まりますか。",
            "Does this rapid stop at Osaka Station?",
            "この かいそくは おおさかえきに ____ か。",
            "とまります",
            ["とまります", "止まります"],
            "Does this rapid stop at Osaka Station?",
        ),
    ],
}

TOPICS = {
    "2026-10-16": (
        "自己紹介[じこしょうかい]",
        "Self-introduction: name, country, city, how you get around, job, birthday.",
    ),
    "2026-10-20": (
        "趣味[しゅみ]と 映画[えいが]",
        "Hobbies, what kind of movies you watch, and how often.",
    ),
    "2026-10-23": (
        "さそいと じかん",
        "Invitations, clock times, and the past tense of verbs.",
    ),
    "2026-10-27": (
        "形容詞[けいようし]",
        "い- and な-adjectives, and describing a place.",
    ),
    "2026-10-30": (
        "形容詞[けいようし]の かこと 方位[ほうい]",
        "Past tense of adjectives, and compass directions.",
    ),
    "2026-11-06": (
        "くらべる",
        "Comparisons and the weather.",
    ),
    "2026-11-10": (
        "欲[ほ]しいと たい",
        "Wanting a thing, and wanting to do something.",
    ),
    "2026-11-13": (
        "て形[けい]",
        "Verb groups, the te-form, and てください.",
    ),
}

NOTES = {
    "2026-10-16": [
        "City spelling on the sheet: サンディエゴ. The small ディ (not ジ) was the sticking point.",
        "The classmate table got filled in.",
    ],
    "2026-10-20": [
        "The sheet header had a Chinese gloss, 頻率[ピンリュー], for the Chinese-speaking classmates. The Japanese word is 頻度[ひんど]. That gloss is not one of Eric's mistakes.",
    ],
    "2026-10-23": [
        "He wrote 'last night' next to 昨夜[ゆうべ].",
        "エリック is written at the top of dialogue No.34.",
    ],
    "2026-10-27": [
        "Board words he marked: 厳[きび]しい, 親切[しんせつ]な, 楽[らく]な, 暇[ひま]な, 北東[ほくとう].",
        "暇[ひま] means free, not busy. He had glossed ひまな as bored. Bored is 退屈[たいくつ].",
        "From the board, he also copied 形容詞[けいようし], 花[はな], 高[たか]い, and 元気[げんき].",
    ],
    "2026-10-30": [
        "He wrote 夕日[ゆうひ] for sunset.",
        "He wrote 親切[しんせつ]な at the top of one sheet.",
        "On the past-tense drill he wrote おもしかった and おもしくなかった. おもしろい keeps the ろ: おもしろかった.",
    ],
    "2026-11-06": [
        "He wrote どこか for somewhere.",
        "He wrote 低[ひく]い under 富士山[ふじさん] in the height drill.",
    ],
    "2026-11-10": [
        "物価[ぶっか] is prices in general. 値段[ねだん] is the price of one thing. He had circled the two, unsure which was which.",
        "On the たい chart he changed にいきます to にいきたいです. 実際[じっさい] means actually.",
    ],
    "2026-11-13": [
        "Memory aid he wrote for one te-form row: い、ち、り → って.",
        "He wrote 写真[しゃしん] above the photo request.",
        "The te-form drill was nearly all correct.",
        "The handout's train dialogue uses station names from the worksheet. Eric's own ride is only what he described: the JR A[エー]線[せん] from 茨木駅[いばらきえき] to 大阪駅[おおさかえき], the 快速[かいそく] when he catches it, then the underground exit M16 and E Street to the YWCA. He has not named the stations in between.",
    ],
}


def choice(cid: str, ruby: str, en: str = "") -> dict:
    return {"id": cid, "ruby": ruby, "en": en}


def fixed(item_id: str, mode: str, prompt: str, en: str, explain_ruby: str, explain_en: str, **rest) -> dict:
    item = {
        "id": item_id,
        "mode": mode,
        "prompt_ruby": prompt,
        "prompt_en": en,
        "explain_ruby": explain_ruby,
        "explain_en": explain_en,
        "choices": rest.get("choices", []),
        "answer_id": rest.get("answer_id", ""),
        "accept": rest.get("accept", []),
        "mistakes": rest.get("mistakes", []),
        "lesson_date": rest.get("lesson_date"),
    }
    return item


WEAK = [
    {
        "id": "hima",
        "title_ruby": "暇[ひま]と 退屈[たいくつ]",
        "reminder_ja": "ひま は いそがしくない。たいくつ は bored。",
        "reminder_en": "ひま means free, not busy. Bored is たいくつ.",
        "weight": 4,
        "word_ids": ["暇な"],
        "items": [
            fixed(
                "w-hima-mc",
                "mc",
                "暇[ひま]な",
                "Pick the meaning.",
                "暇[ひま]は いそがしくない。bored は 退屈[たいくつ]。",
                "Free, not busy. Bored is たいくつ.",
                lesson_date="2026-10-27",
                answer_id="free",
                choices=[
                    choice("free", "いそがしくない", "free; not busy"),
                    choice("bored", "することが なくて、あきる", "bored"),
                    choice("sleepy", "ねむい", "sleepy"),
                    choice("strict", "きびしい", "strict"),
                ],
            ),
            fixed(
                "w-taikutsu-mc",
                "mc",
                "退屈[たいくつ]",
                "This is the word for bored. It is not ひま.",
                "退屈[たいくつ]が bored。暇[ひま]は じかんが ある、ということ。",
                "たいくつ is bored. ひま is having time.",
                lesson_date="2026-10-27",
                answer_id="bored",
                choices=[
                    choice("bored", "あきて、たいくつ", "bored"),
                    choice("free", "いそがしくない", "free; not busy"),
                    choice("easy", "らく", "easy"),
                    choice("fun", "おもしろい", "fun"),
                ],
            ),
            fixed(
                "w-hima-fill",
                "fill",
                "いそがしくない。じかんが ある。",
                "Type the word in kana.",
                "暇[ひま]。たいくつでは ない。",
                "ひま. Not たいくつ.",
                lesson_date="2026-10-27",
                accept=["ひま", "ひまな"],
                mistakes=[{"answer": "たいくつ", "hint_en": "たいくつ is bored. Free, not busy, is ひま."}],
            ),
        ],
    },
    {
        "id": "price",
        "title_ruby": "物価[ぶっか]と 値段[ねだん]",
        "reminder_ja": "ぶっか は まち ぜんたい。ねだん は ひとつの もの。",
        "reminder_en": "ぶっか is prices in general. ねだん is the price of one thing.",
        "weight": 4,
        "word_ids": ["物価", "値段"],
        "items": [
            fixed(
                "w-bukka-mc",
                "mc",
                "物価[ぶっか]",
                "Which meaning fits?",
                "物価[ぶっか]は まち ぜんたいの ねだん。ひとつの ものは 値段[ねだん]。",
                "Cost of living, prices in general. One item is ねだん.",
                lesson_date="2026-11-10",
                answer_id="general",
                choices=[
                    choice("general", "まち ぜんたいの ねだん", "prices in general"),
                    choice("one", "ひとつの ものの ねだん", "price of one item"),
                    choice("actual", "じっさい", "actually"),
                    choice("cheap", "やすい", "cheap"),
                ],
            ),
            fixed(
                "w-nedan-mc",
                "mc",
                "値段[ねだん]",
                "Which meaning fits?",
                "値段[ねだん]は その ひとつの ねだん。まち ぜんたいは 物価[ぶっか]。",
                "The price of one thing. The general level is ぶっか.",
                lesson_date="2026-11-10",
                answer_id="one",
                choices=[
                    choice("one", "ひとつの ものの ねだん", "price of one item"),
                    choice("general", "まち ぜんたいの ねだん", "prices in general"),
                    choice("wallet", "おかね", "money"),
                    choice("high", "たかい", "expensive"),
                ],
            ),
            fixed(
                "w-bukka-fill",
                "fill",
                "まち ぜんたいで、ものの ねだんが たかいか やすいか。",
                "Type the reading.",
                "物価[ぶっか]。",
                "ぶっか.",
                lesson_date="2026-11-10",
                accept=["ぶっか"],
                mistakes=[{"answer": "ねだん", "hint_en": "ねだん is one item. Prices in general are ぶっか."}],
            ),
            fixed(
                "w-nedan-fill",
                "fill",
                "その ひとつの ものの ねだん。",
                "Type the reading.",
                "値段[ねだん]。",
                "ねだん.",
                lesson_date="2026-11-10",
                accept=["ねだん"],
                mistakes=[{"answer": "ぶっか", "hint_en": "ぶっか is the general level. One item is ねだん."}],
            ),
        ],
    },
    {
        "id": "small-kana",
        "title_ruby": "ちいさい かな（ディ）",
        "reminder_ja": "ディ は ちいさい ィ。ジ では ない。サンディエゴ。",
        "reminder_en": "ディ uses a small ィ. San Diego is サンディエゴ, not サンジエゴ.",
        "weight": 4,
        "word_ids": [],
        "items": [
            fixed(
                "w-di-mc",
                "mc",
                "サンディエゴの 「di」は どれ？",
                "The small-kana combo.",
                "ちいさい ィ を つかう。ディ。",
                "Small ィ: ディ, not ジ.",
                lesson_date="2026-10-16",
                answer_id="di",
                choices=[
                    choice("di", "ディ", "small ィ"),
                    choice("ji", "ジ", "big ジ"),
                    choice("di2", "ヂ", "another big kana"),
                    choice("dei", "でい", "で + big い"),
                ],
            ),
            fixed(
                "w-sd-mc",
                "mc",
                "アメリカの まち。どれが サンディエゴ？",
                "Pick the spelling from class.",
                "サンディエゴ。ディ を おおきく しない。",
                "サンディエゴ.",
                lesson_date="2026-10-16",
                answer_id="right",
                choices=[
                    choice("right", "サンディエゴ", "small ィ"),
                    choice("ji", "サンジエゴ", "big ジ"),
                    choice("di", "サンヂエゴ", "ヂ"),
                    choice("hira", "さんじえご", "no small kana"),
                ],
            ),
            fixed(
                "w-sd-fill",
                "fill",
                "アメリカの ____ から きました。",
                "San Diego, the way it was corrected in class.",
                "サンディエゴ。",
                "サンディエゴ.",
                lesson_date="2026-10-16",
                accept=["サンディエゴ"],
                mistakes=[
                    {"answer": "サンジエゴ", "hint_en": "Small ィ. ディ, not ジ."},
                    {"answer": "さんじえご", "hint_en": "Small ィ. サンディエゴ."},
                ],
            ),
        ],
    },
    {
        "id": "omoshirokatta",
        "title_ruby": "おもしろかった",
        "reminder_ja": "おもしろい の かこは おもしろかった。ろ を けさない。",
        "reminder_en": "Past of おもしろい is おもしろかった. The ろ stays.",
        "weight": 4,
        "word_ids": [],
        "items": [
            fixed(
                "w-omo-mc",
                "mc",
                "えいがは ____ です。おもしろい の かこ。",
                "Pick the past tense.",
                "おもしろかった。ろ が のこる。",
                "おもしろかった. The ろ stays.",
                lesson_date="2026-10-30",
                answer_id="right",
                choices=[
                    choice("right", "おもしろかった", "the ろ stays"),
                    choice("drop", "おもしかった", "missing ろ"),
                    choice("datta", "おもしろいだった", "だった does not attach"),
                    choice("ku", "おもしろくかった", "extra く"),
                ],
            ),
            fixed(
                "w-omo-fill",
                "fill",
                "きのうの えいがは ____。",
                "How was the movie? Past of おもしろい.",
                "おもしろかった。おもしかった では ない。",
                "おもしろかった, not おもしかった.",
                lesson_date="2026-10-30",
                accept=["おもしろかった", "おもしろかったです"],
                mistakes=M_OMOSHIRO,
            ),
        ],
    },
]


def is_han(ch: str) -> bool:
    o = ord(ch)
    return 0x4E00 <= o <= 0x9FFF or 0x3400 <= o <= 0x4DBF or ch in "々〆"


def parse_ruby(text: str) -> list[tuple[str, str | None]]:
    out: list[tuple[str, str | None]] = []
    i = 0
    while i < len(text):
        bracket = text.find("[", i)
        if bracket < 0:
            out.append((text[i:], None))
            break
        end = text.find("]", bracket)
        if end < 0:
            out.append((text[i:], None))
            break
        reading = text[bracket + 1 : end]
        base_start = bracket
        if base_start > i and is_han(text[base_start - 1]):
            while base_start > i and is_han(text[base_start - 1]):
                base_start -= 1
        elif base_start > i:
            while base_start > i and (not text[base_start - 1].isspace()) and text[base_start - 1] != "]":
                base_start -= 1
        if base_start > i:
            out.append((text[i:base_start], None))
        out.append((text[base_start:bracket], reading))
        i = end + 1
    return [(base, reading) for base, reading in out if base or reading]


def bare_kanji(text: str) -> list[str]:
    found = []
    for base, reading in parse_ruby(text):
        if reading is None:
            hans = [ch for ch in base if is_han(ch)]
            if hans:
                found.append("".join(hans))
        elif any(is_han(ch) for ch in reading):
            found.append(reading)
    return found


def teacher_of(raw: str, date: str) -> tuple[str, bool]:
    if date == "2026-11-13":
        return "ouchi", True
    if "ツジ" in raw or "Tsuji" in raw:
        return "tsuji", False
    if "大内" in raw or "Ōuchi" in raw or "Ouchi" in raw:
        return "ouchi", False
    return "ouchi", True


def clean_en(meaning: str) -> str:
    # Drop export asides that treat the Chinese gloss as Eric's error.
    text = meaning
    for cut in (" (Eric", " (worksheet", " (sheet"):
        if cut in text:
            text = text.split(cut)[0]
    return text.strip()


def main() -> None:
    curriculum = json.loads((SOURCE / "curriculum.json").read_text())
    lessons_src = json.loads((SOURCE / "lessons.json").read_text())
    wordbank = json.loads((SOURCE / "wordbank.json").read_text())

    by_date_lesson = {row["date"]: row for row in lessons_src["lessons"]}
    lessons = []
    for row in curriculum["lessons"]:
        date = row["date"] or "2026-11-13"
        meta = by_date_lesson[date]
        teacher_id, inferred = teacher_of(str(row.get("teacher") or meta["teacher"]), date)
        topic_ruby, topic_en = TOPICS[date]
        lessons.append(
            {
                "id": date,
                "date": date,
                "weekday": meta["weekday"],
                "teacher_id": teacher_id,
                "teacher_inferred": inferred or bool(meta.get("date_inferred")),
                "date_inferred": bool(meta.get("date_inferred")) or row.get("date") is None,
                "anki_tag": meta["anki_tag"],
                "topic_ruby": topic_ruby,
                "topic_en": topic_en,
                "source": row["source"],
                "pages": row["pages"],
                "grammar": GRAMMAR[date],
                "notes": NOTES[date],
            }
        )

    words = []
    seen = set()
    for row in wordbank["words"]:
        word = row["word"]
        if word in seen:
            raise SystemExit(f"duplicate word {word}")
        seen.add(word)
        if word not in GLOSSES:
            raise SystemExit(f"missing gloss for {word}")
        gloss_ja, watch, accept = GLOSSES[word]
        status = row["status"]
        status_note = None
        if word == "頻度":
            # Handwriting here was the teacher's Chinese gloss, not a study mark.
            status = "known"
            status_note = "chinese_gloss_for_classmates"
        item = {
            "id": word,
            "word": word,
            "ruby": row["word_furigana"],
            "kana": row["kana"],
            "gloss_ja": gloss_ja,
            "gloss_en": clean_en(row["meaning"]),
            "status": status,
            "lesson_date": row["class_date"] or "2026-11-13",
            "teacher_id": {"ouchi": "ouchi", "tsuji": "tsuji"}.get(row.get("teacher") or "", "ouchi"),
            "source_sheet": row.get("source_sheet") or "",
            "accept": accept,
        }
        if watch:
            item["watch_out"] = watch
        if status_note:
            item["status_note"] = status_note
        words.append(item)

    missing = set(GLOSSES) - seen
    if missing:
        raise SystemExit(f"glosses with no word: {sorted(missing)}")

    profile = {
        "schema_version": 1,
        "student": "Eric Allerton",
        "city_ruby": "茨木[いばらき]市[し]",
        "city_en": "Ibaraki-shi, Osaka",
        "school_ruby": "大阪[おおさか]YWCA 日本語[にほんご]学科[がっか]",
        "school_en": "Osaka YWCA, Japanese department, evening class 2. Tue and Fri 19:00–20:30.",
        "anki_deck": "YWCA",
        "anki_tag_prefix": "class::",
        "kanji_study": "not_started",
        "commute": {
            "summary_ruby": "JRの A[エー]線[せん]で、茨木駅[いばらきえき]から 大阪駅[おおさかえき]まで。快速[かいそく]に 間[ま]に 合[あ]えば 快速[かいそく]。おりたら、地下[ちか]の 出口[でぐち] M16 から Eストリートを 歩[ある]いて YWCA。",
            "summary_en": "JR A-line from Ibaraki Station to Osaka Station. The rapid, if he catches it. Then underground exit M16 and E Street to the YWCA.",
        },
        "schedule": [
            {
                "weekday": "Tue",
                "start": "19:00",
                "end": "20:30",
                "teacher_id": "tsuji",
                "focus_en": "dialogues",
                "focus_ruby": "かいわ",
            },
            {
                "weekday": "Fri",
                "start": "19:00",
                "end": "20:30",
                "teacher_id": "ouchi",
                "focus_en": "grammar worksheets",
                "focus_ruby": "ぶんぽうの プリント",
            },
        ],
        "teachers": [
            {
                "id": "tsuji",
                "name_ruby": "ツジ",
                "name_en": "Tsuji",
                "honorific_ruby": "せんせい",
            },
            {
                "id": "ouchi",
                "name_ruby": "大内[おおうち]",
                "name_en": "Ōuchi",
                "honorific_ruby": "せんせい",
            },
        ],
        "weak_spots": WEAK,
    }

    # Kanji gate. gloss_ja and explanation_ja and reminder_ja are kana-only.
    kana_fields_errors = []
    ruby_errors = []

    def check_kana(label: str, text: str) -> None:
        hans = [ch for ch in text if is_han(ch)]
        if hans:
            kana_fields_errors.append(f"{label}: {''.join(hans)} in {text}")

    def check_ruby(label: str, text: str) -> None:
        bad = bare_kanji(text)
        if bad:
            ruby_errors.append(f"{label}: bare {bad} in {text}")

    check_ruby("commute", profile["commute"]["summary_ruby"])
    check_ruby("city", profile["city_ruby"])
    check_ruby("school", profile["school_ruby"])
    for spot in WEAK:
        check_ruby(spot["id"], spot["title_ruby"])
        check_kana(spot["id"], spot["reminder_ja"])
        for item in spot["items"]:
            check_ruby(item["id"], item["prompt_ruby"])
            check_ruby(item["id"], item["explain_ruby"])
            for c in item["choices"]:
                check_ruby(item["id"] + "/" + c["id"], c["ruby"])

    for lesson in lessons:
        check_ruby(lesson["id"] + " topic", lesson["topic_ruby"])
        for note in lesson["notes"]:
            check_ruby(lesson["id"] + " note", note)
        for point in lesson["grammar"]:
            for key in ("pattern_ruby", "example_ruby", "cloze_ruby"):
                check_ruby(point["id"] + " " + key, point[key])
            check_kana(point["id"], point["explanation_ja"])

    for word in words:
        check_kana(word["id"], word["gloss_ja"])
        check_ruby(word["id"], word["ruby"])
        if "watch_out" in word:
            check_ruby(word["id"] + " watch", word["watch_out"])

    if kana_fields_errors or ruby_errors:
        raise SystemExit("\n".join(kana_fields_errors + ruby_errors))

    banned = ("吹田", "梅田", "すいた", "うめだ", "新大阪", "しんおおさか", "Umeda", "Suita")
    blob_parts = [
        profile["commute"]["summary_ruby"],
        profile["commute"]["summary_en"],
        profile["school_en"],
    ]
    for lesson in lessons:
        blob_parts.extend(lesson["notes"])
        for point in lesson["grammar"]:
            blob_parts.extend(
                [
                    point["pattern_ruby"],
                    point["explanation_ja"],
                    point["explanation_en"],
                    point["example_ruby"],
                    point["example_en"],
                    point["cloze_ruby"],
                    point["cloze_hint_en"],
                ]
            )
    blob = "\n".join(blob_parts)
    hits = [word for word in banned if word in blob]
    if hits:
        raise SystemExit(f"invented stop or Umeda in authored copy: {hits}")

    for spot in WEAK:
        joined = json.dumps(spot, ensure_ascii=False)
        for bad in ("頻度", "頻率", "ひんどう", "ひんぱん"):
            if bad in joined:
                raise SystemExit(f"weak spot still mentions {bad}")

    study = [w for w in words if w["status"] == "study"]
    if any(w["id"] == "頻度" and w["status"] != "known" for w in words):
        raise SystemExit("頻度 should be known")
    if len(words) != 237:
        raise SystemExit(f"word count {len(words)}")
    grammar_n = sum(len(lesson["grammar"]) for lesson in lessons)
    if grammar_n != 40:
        raise SystemExit(f"grammar count {grammar_n}")
    if len(study) != 25:
        raise SystemExit(f"study count {len(study)} {[w['id'] for w in study]}")

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "profile.json").write_text(json.dumps(profile, ensure_ascii=False, indent=2) + "\n")
    (OUT / "lessons.json").write_text(
        json.dumps({"schema_version": 1, "lessons": lessons}, ensure_ascii=False, indent=2) + "\n"
    )
    (OUT / "words.json").write_text(
        json.dumps({"schema_version": 1, "words": words}, ensure_ascii=False, indent=2) + "\n"
    )
    print(f"wrote {len(lessons)} lessons, {grammar_n} grammar points, {len(words)} words, {len(study)} study")


if __name__ == "__main__":
    main()
